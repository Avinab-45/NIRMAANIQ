# NirmaanIQ — YOLO11 FastAPI ML Service Integration Contract & Guide

This document defines the architectural contract, environment configuration, payload schemas, and testing instructions for integrating the custom-trained YOLO11 computer vision model (Python FastAPI microservice) with the NirmaanIQ Spring Boot 3.3.5 / Java 21 backend.

---

## 1. Architecture & Division of Responsibility

| Component | Responsibility Owner | Technology Stack |
|---|---|---|
| **Transactional Backend & Auth** | Spring Boot Developer | Java 21, Spring Boot 3.3.5, Spring Security, Maven |
| **Database & File Storage** | Spring Boot Developer | Supabase PostgreSQL, Supabase Storage (`construction-media`) |
| **Model Training & Inference API** | ML Teammate | Python 3.10+, PyTorch, Ultralytics YOLO11, FastAPI, Uvicorn |

```
 +-----------------------------------------------------------------------------------+
 |                                 NirmaanIQ System                                  |
 +-----------------------------------------------------------------------------------+
 |                                                                                   |
 |  +---------------------+             +------------------------------------------+ |
 |  |   Client / Frontend |             |        Private Supabase Storage          | |
 |  |   (Web / Mobile)    |             |        Bucket: 'construction-media'      | |
 |  +----------+----------+             +--------------------+---------------------+ |
 |             |                                             |                       |
 |    1. Upload photo                               Private storage download         |
 |    2. Request /analyze                                    |                       |
 |             |                                             v                       |
 |             v                                +--------------------------+         |
 |  +-------------------------------------------+   Spring Boot Backend    |         |
 |  |                                           |   (Java 21 / 3.3.5)      |         |
 |  |  - Auth & Strict Role Access Control      +------------+-------------+         |
 |  |  - In-Memory Media Proxy                               |                       |
 |  |  - Idempotency & Lifecycle Management                  |                       |
 |  |  - Persistent Table: 'media_ai_analyses'               | 3. POST /predict      |
 |  +--------------------------------------------------------+    (Multipart Form)   |
 |                                                           |                       |
 |                                                           v                       |
 |                                              +--------------------------+         |
 |                                              |      Python FastAPI      |         |
 |                                              |   (YOLO11 Microservice)  |         |
 |                                              +--------------------------+         |
 +-----------------------------------------------------------------------------------+
```

### Core Architecture Principles:
1. **Decoupled Lifecycle**: The Spring Boot backend starts and operates normally regardless of whether the FastAPI ML microservice is online, offline, or not yet deployed.
2. **Zero Direct Supabase Exposure**: The FastAPI microservice **never** connects to Supabase, has no database credentials, and receives binary image streams directly from the Spring Boot backend in-memory.
3. **Idempotency Guard**: Repeat requests to analyze an already analyzed photograph return the cached `COMPLETED` record instantly without re-calling FastAPI. Passing `?reanalyze=true` forces a fresh inference.
4. **Strict Separation of Concerns**: YOLO11 detections and visual completion estimates are stored exclusively in `media_ai_analyses`. They **never** overwrite or alter manual contractor progress records (`area_completed`, `work_category`, etc.).

---

## 2. API Contract: Spring Boot -> FastAPI

### 2.1 HTTP Request Specification

- **Method**: `POST`
- **Path**: `${ML_SERVICE_PREDICT_ENDPOINT}` (Default: `/predict`)
- **Base URL**: `${ML_SERVICE_URL}` (Default: `http://localhost:8000`)
- **Content-Type**: `multipart/form-data`
- **Headers**:
  - `X-API-Key`: `<API_KEY>` (Sent when `ML_SERVICE_API_KEY` is configured)
  - `Authorization`: `Bearer <API_KEY>` (Sent when `ML_SERVICE_API_KEY` is configured)

#### Form-Data Fields:

| Field Name | Type | Required | Description |
|---|---|---|---|
| `${ML_SERVICE_FILE_PARAM_NAME}` | Binary File | **Yes** | The site photograph (Default field name: `file`). |
| `project_code` | String | No | NirmaanIQ project code (e.g. `PRJ-BLR-001`). |
| `media_id` | String (UUID) | No | Unique ID of the photo in `media_files`. |

---

### 2.2 Expected FastAPI JSON Response Schema

The backend uses Jackson `@JsonAlias` mappings to accept flexible naming conventions from your FastAPI service.

```json
{
  "model": "yolo11x-construction",
  "version": "1.0.0",
  "status": "COMPLETED",
  "summary": "Foundation rebar grid and timber formwork detected with high confidence.",
  "work_category": "FOUNDATION",
  "estimated_completion_percentage": 65.5,
  "detections": [
    {
      "class_name": "rebar_grid",
      "confidence": 0.94,
      "box": {
        "x1": 120.5,
        "y1": 80.0,
        "x2": 450.0,
        "y2": 380.0
      },
      "attributes": {
        "spacing": "standard",
        "corrosion": false
      }
    },
    {
      "class_name": "concrete_formwork",
      "confidence": 0.88,
      "box": [50.0, 60.0, 500.0, 400.0]
    }
  ],
  "issues": [
    "Minor pooling of rainwater in eastern footing trench",
    "Missing perimeter safety barricade"
  ],
  "recommendations": [
    "Pump out pooled groundwater prior to structural concrete pouring",
    "Install OSHA-compliant warning barriers along trench boundary"
  ],
  "metadata": {
    "inference_time_ms": 42.8,
    "device": "cuda:0"
  }
}
```

#### Field Aliases Supported by Backend:

| Target Field | Accepted JSON Keys | Type | Notes |
|---|---|---|---|
| `model` | `model`, `model_name`, `model_id`, `name` | String | Model identifier |
| `version` | `version`, `model_version`, `checkpoint` | String | Weights version |
| `status` | `status`, `state`, `result_status` | String | `COMPLETED` or `FAILED` |
| `summary` | `summary`, `analysis_summary`, `description`, `message`, `text` | String | Human-readable summary |
| `work_category` | `work_category`, `workCategory`, `category`, `stage`, `class` | String | Construction stage |
| `estimated_completion_percentage` | `estimated_completion_percentage`, `completion_percentage`, `progress_percentage`, `percentage`, `progress` | Number (BigDecimal) | Visual progress % |
| `detections` | `detections`, `predictions`, `objects`, `boxes`, `results`, `data`, `items` | Array | Object detections |
| `detections[].class_name` | `class`, `class_name`, `label`, `category`, `name` | String | Object class label |
| `detections[].confidence` | `confidence`, `score`, `conf`, `prob` | Number (Double) | 0.0 to 1.0 |
| `detections[].box` | `box`, `bbox`, `bounding_box`, `coordinates` | Object OR Array | `{x1, y1, x2, y2}` or `[x1, y1, x2, y2]` |
| `issues` | `issues`, `detected_issues`, `hazards`, `violations`, `defects`, `warnings`, `alerts` | Array of Strings | Safety/quality issues |
| `recommendations` | `recommendations`, `suggestions`, `actions`, `remedies` | Array of Strings | Recommended actions |
| `metadata` | `metadata`, `extra`, `info`, `diagnostics`, `stats` | Object (Map) | Diagnostic details |

> **Audit Trail Note**: Regardless of which fields are populated, the entire raw JSON response is saved verbatim in the PostgreSQL `media_ai_analyses.raw_analysis` (JSONB) column for model auditing and retraining.

---

## 3. Backend Configuration Variables

Configure these in `application.properties` or `.env`:

```properties
# ===================================================================
# YOLO11 FastAPI ML Service Configuration
# ===================================================================
# Toggle ML integration (default: true). If false, no HTTP calls are attempted.
ML_SERVICE_ENABLED=true

# Root URL of your FastAPI service
ML_SERVICE_URL=http://localhost:8000

# Optional shared secret API key (sent in X-API-Key and Authorization: Bearer headers)
ML_SERVICE_API_KEY=your-shared-secret-key

# Prediction endpoint path
ML_SERVICE_PREDICT_ENDPOINT=/predict

# Name of the multipart form parameter for the image file (default: file)
ML_SERVICE_FILE_PARAM_NAME=file

# Connection timeout in seconds
ML_SERVICE_CONNECT_TIMEOUT=5

# Inference execution read timeout in seconds
ML_SERVICE_READ_TIMEOUT=30
```

---

## 4. Deliverables Checklist for the ML Teammate

When you are ready to integrate your FastAPI service with the backend, provide the following:

- [ ] **1. Running FastAPI Service**: Accessible at `ML_SERVICE_URL` (locally e.g. `http://localhost:8000` or hosted URL).
- [ ] **2. Predict Endpoint**: `POST /predict` receiving multipart image payload.
- [ ] **3. Shared Secret Key**: If authentication is enabled, provide the string for `ML_SERVICE_API_KEY`.
- [ ] **4. Model Class Labels List**: Documentation of classes detected by your YOLO11 model (e.g. `rebar_grid`, `concrete_formwork`, `excavator`, `safety_barrier`, etc.).
- [ ] **5. Health Check Endpoint**: `GET /health` returning `{"status": "UP"}`.

---

## 5. Standalone FastAPI Reference Implementation (`main.py`)

You can run this self-contained script immediately to verify end-to-end integration:

```python
import io
import os
import time
from typing import List, Optional, Dict, Any
from fastapi import FastAPI, File, UploadFile, Form, Header, HTTPException, status
from pydantic import BaseModel
from PIL import Image
from ultralytics import YOLO

app = FastAPI(
    title="NirmaanIQ YOLO11 Construction Vision API",
    version="1.0.0",
    description="Inference microservice for site photograph analysis"
)

# Configuration
API_KEY = os.getenv("ML_SERVICE_API_KEY", "")
MODEL_PATH = os.getenv("YOLO_MODEL_PATH", "yolo11n.pt")
CONF_THRESHOLD = float(os.getenv("CONFIDENCE_THRESHOLD", "0.30"))

print(f"Loading YOLO11 model: {MODEL_PATH}")
model = YOLO(MODEL_PATH)
print("Model ready for inference.")

class BoundingBox(BaseModel):
    x1: float
    y1: float
    x2: float
    y2: float

class Detection(BaseModel):
    class_name: str
    confidence: float
    box: BoundingBox
    attributes: Optional[Dict[str, Any]] = None

class PredictResponse(BaseModel):
    model: str
    version: str
    status: str
    summary: str
    work_category: Optional[str] = None
    estimated_completion_percentage: Optional[float] = None
    detections: List[Detection] = []
    issues: List[str] = []
    recommendations: List[str] = []
    metadata: Dict[str, Any] = {}

@app.get("/health")
def health_check():
    return {"status": "UP", "model": MODEL_PATH}

@app.post("/predict", response_model=PredictResponse)
async def predict_photo(
    file: UploadFile = File(...),
    project_code: Optional[str] = Form(None),
    media_id: Optional[str] = Form(None),
    x_api_key: Optional[str] = Header(None, alias="X-API-Key"),
    authorization: Optional[str] = Header(None)
):
    # Verify shared secret if configured
    if API_KEY:
        token = x_api_key or (authorization.split(" ")[1] if authorization and authorization.startswith("Bearer ") else None)
        if token != API_KEY:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid API Key")

    start_time = time.time()

    try:
        image_bytes = await file.read()
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    except Exception as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=f"Image decoding failed: {str(e)}")

    # Execute YOLO11 Inference
    try:
        results = model.predict(source=image, conf=CONF_THRESHOLD)
        result = results[0]
    except Exception as e:
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=f"Inference error: {str(e)}")

    detections: List[Detection] = []
    for box in result.boxes:
        cls_id = int(box.cls[0].item())
        cls_name = model.names.get(cls_id, f"class_{cls_id}")
        conf = round(float(box.conf[0].item()), 4)
        xyxy = box.xyxy[0].tolist()

        detections.append(Detection(
            class_name=cls_name,
            confidence=conf,
            box=BoundingBox(
                x1=round(xyxy[0], 2),
                y1=round(xyxy[1], 2),
                x2=round(xyxy[2], 2),
                y2=round(xyxy[3], 2)
            )
        ))

    inference_ms = round((time.time() - start_time) * 1000, 2)

    return PredictResponse(
        model=f"yolo11-{os.path.basename(MODEL_PATH)}",
        version="1.0.0",
        status="COMPLETED",
        summary=f"Detected {len(detections)} construction elements.",
        work_category="FOUNDATION",
        estimated_completion_percentage=60.0,
        detections=detections,
        issues=["No perimeter safety barricades detected"],
        recommendations=["Install safety warning barriers"],
        metadata={"inference_time_ms": inference_ms, "media_id": media_id}
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
```

---

## 6. End-to-End Verification Instructions

### Step 1: Start FastAPI
```bash
python main.py
# Running on http://localhost:8000
```

### Step 2: Trigger Analysis Through Spring Boot Backend
```bash
# Upload a photo to the project
curl -X POST http://localhost:8080/api/projects/PRJ-001/media \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -F "file=@site_photo.jpg" \
  -F "category=SITE_PHOTO"

# Trigger AI analysis for the uploaded photo UUID
curl -X POST http://localhost:8080/api/projects/PRJ-001/media/<mediaId>/analyze \
  -H "Authorization: Bearer <JWT_TOKEN>"

# Verify idempotency (repeat call returns cached result immediately)
curl -X POST http://localhost:8080/api/projects/PRJ-001/media/<mediaId>/analyze \
  -H "Authorization: Bearer <JWT_TOKEN>"

# Force re-analysis
curl -X POST "http://localhost:8080/api/projects/PRJ-001/media/<mediaId>/analyze?reanalyze=true" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```
