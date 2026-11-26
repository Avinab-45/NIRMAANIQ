# NirmaanIQ API Documentation

This document describes all REST API endpoints implemented in the Construction Intelligence backend.

Base URL: `http://localhost:8080/api`

---

## Standard Response Format

All responses follow a consistent envelope structure:

### Success Response
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2026-11-15T22:00:00"
}
```

### Error Response
```json
{
  "success": false,
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid input parameters",
  "path": "/api/projects",
  "validationErrors": {
    "name": "Project name is required"
  },
  "timestamp": "2026-11-15T22:00:00"
}
```

---

## 1. Authentication & System Health

### 1.1 Health Check
- **Endpoint**: `GET /api/health`
- **Auth**: Public
- **Description**: Performs an active database connection check (`SELECT 1`) and returns server status, service name, version, and database connection state.
- **Response (Connected)**: `200 OK`
```json
{
  "success": true,
  "message": "Service is running smoothly",
  "data": {
    "status": "UP",
    "service": "construction-backend",
    "version": "1.0.0",
    "timestamp": "2026-11-19T12:00:00",
    "database": "Supabase PostgreSQL (Connected)"
  },
  "timestamp": "2026-11-19T12:00:00"
}
```
- **Response (Disconnected)**: `503 Service Unavailable`
```json
{
  "success": false,
  "message": "Database connection is unavailable",
  "data": {
    "status": "DOWN",
    "service": "construction-backend",
    "version": "1.0.0",
    "timestamp": "2026-11-19T12:00:00",
    "database": "Supabase PostgreSQL (Disconnected)"
  },
  "timestamp": "2026-11-19T12:00:00"
}
```

### 1.2 Builder Activation
- **Endpoint**: `POST /api/auth/builder/activate`
- **Auth**: Authenticated (Supabase JWT in `Authorization: Bearer <token>`)
- **Description**: Activates a registered builder account using an invitation/activation secret code. Assigns `ROLE_BUILDER` and sets status to `ACTIVE`.
- **Request Body**:
```json
{
  "activationCode": "BUILDER-ACTIVATE-SECRET-2026",
  "companyName": "Apex Infrastructure Ltd",
  "licenseNumber": "BLD-998822"
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Builder account activated successfully.",
  "data": {
    "id": "c39988fd-5ca8-471d-906c-1be9d8ad3d5c",
    "email": "builder@apex.com",
    "fullName": "Avinab Builder",
    "status": "ACTIVE",
    "roles": ["ROLE_BUILDER"],
    "createdAt": "2026-11-15T21:40:00"
  },
  "timestamp": "2026-11-15T22:00:00"
}
```

### 1.3 Builder Profile
- **Endpoint**: `GET /api/auth/builder/me`
- **Auth**: `ROLE_BUILDER`
- **Response**: `200 OK`

### 1.4 Contractor Request OTP
- **Endpoint**: `POST /api/auth/contractor/request-otp`
- **Auth**: Authenticated (`Authorization: Bearer <contractor-supabase-jwt>`)
- **Description**: Requests a 6-digit email OTP for secondary verification. The email provided must match the authenticated user's verified email. Enforces a 60-second cooldown between requests and a 5-minute expiry.
- **Request Body**:
```json
{
  "email": "contractor@buildfast.com"
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Verification OTP has been sent to your registered email.",
  "data": null,
  "timestamp": "2026-11-15T22:00:00"
}
```

### 1.5 Contractor Verify OTP
- **Endpoint**: `POST /api/auth/contractor/verify-otp`
- **Auth**: Authenticated (`Authorization: Bearer <contractor-supabase-jwt>`)
- **Description**: Verifies the 6-digit email OTP against the secure hash in `otp_challenges`. The email provided must match the authenticated user's verified email. One-time use. Maximum 5 attempts. Activates contractor profile with `ROLE_CONTRACTOR`.
- **Request Body**:
```json
{
  "email": "contractor@buildfast.com",
  "otp": "481920"
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Contractor verified successfully.",
  "data": {
    "id": "e49988fd-5ca8-471d-906c-1be9d8ad3d5c",
    "email": "contractor@buildfast.com",
    "status": "ACTIVE",
    "roles": ["ROLE_CONTRACTOR"],
    "createdAt": "2026-11-15T21:45:00"
  },
  "timestamp": "2026-11-15T22:00:00"
}
```

### 1.6 Client Profile
- **Endpoint**: `GET /api/auth/client/me`
- **Auth**: Authenticated Client (Supabase Email OTP / JWT with verified email)
- **Description**: Returns the authenticated client profile. The client logs in via Supabase Email OTP on the frontend. The backend validates the JWT and verified email claim, ensures `ROLE_CLIENT` and `ACTIVE` status.
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Profile retrieved successfully",
  "data": {
    "id": "f59988fd-5ca8-471d-906c-1be9d8ad3d5c",
    "email": "client@example.com",
    "fullName": "Priya Sharma",
    "phoneNumber": "+919876543210",
    "status": "ACTIVE",
    "roles": ["ROLE_CLIENT"],
    "createdAt": "2026-11-15T21:50:00"
  },
  "timestamp": "2026-11-15T22:00:00"
}
```

---

## 2. Projects

### 2.1 Create Project
- **Endpoint**: `POST /api/projects`
- **Auth**: `ROLE_BUILDER` (Active account)
- **Description**: Builder creates a project. The backend auto-generates a unique project code (e.g. `PRJ-9X8K2M`) and assigns the builder as `OWNER` in `project_members`.
- **Request Body**:
```json
{
  "name": "Skyline Heights Tower B",
  "description": "24-story residential development with smart amenities",
  "location": "Sector 62, Gurgaon",
  "plannedStartDate": "2026-12-01",
  "plannedEndDate": "2028-06-30"
}
```
- **Response**: `201 Created`
```json
{
  "success": true,
  "message": "Project created successfully",
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "projectCode": "PRJ-9X8K2M",
    "name": "Skyline Heights Tower B",
    "description": "24-story residential development with smart amenities",
    "location": "Sector 62, Gurgaon",
    "status": "PLANNING",
    "plannedStartDate": "2026-12-01",
    "plannedEndDate": "2028-06-30",
    "createdById": "c39988fd-5ca8-471d-906c-1be9d8ad3d5c",
    "createdByName": "Avinab Builder",
    "createdAt": "2026-11-15T22:00:00"
  },
  "timestamp": "2026-11-15T22:00:00"
}
```

### 2.2 List Projects
- **Endpoint**: `GET /api/projects?page=0&size=10`
- **Auth**: Authenticated (`BUILDER`, `CONTRACTOR`, `CLIENT`)
- **Description**: Returns paginated projects. Builders see owned projects; Contractors & Clients see assigned active projects.
- **Response**: `200 OK`

### 2.3 Get Project by Code
- **Endpoint**: `GET /api/projects/{projectCode}`
- **Auth**: Assigned active member or owner builder
- **Response**: `200 OK`

### 2.4 Update Project
- **Endpoint**: `PUT /api/projects/{projectCode}`
- **Auth**: Builder owner only
- **Request Body**:
```json
{
  "name": "Skyline Heights Luxury Tower B",
  "status": "IN_PROGRESS",
  "actualStartDate": "2026-12-05"
}
```
- **Response**: `200 OK`

### 2.5 Archive Project
- **Endpoint**: `DELETE /api/projects/{projectCode}`
- **Auth**: Builder owner only
- **Response**: `200 OK`

---

## 3. Project Members

### 3.1 Assign Member
- **Endpoint**: `POST /api/projects/{projectCode}/members`
- **Auth**: Builder owner only
- **Description**:
  - Assign contractor using registered verified email (user must have completed contractor OTP verification).
  - Assign client using registered verified email (auto-provisions `ROLE_CLIENT` in `user_roles`; optional `phoneNumber` can be stored as contact info).
- **Request Body (Contractor)**:
```json
{
  "role": "CONTRACTOR",
  "email": "contractor@buildfast.com"
}
```
- **Request Body (Client)**:
```json
{
  "role": "CLIENT",
  "email": "client@example.com",
  "phoneNumber": "+919876543210"
}
```
- **Response**: `201 Created`

### 3.2 List Members
- **Endpoint**: `GET /api/projects/{projectCode}/members`
- **Auth**: Assigned member or owner builder
- **Response**: `200 OK`

### 3.3 Remove Member
- **Endpoint**: `DELETE /api/projects/{projectCode}/members/{userId}`
- **Auth**: Builder owner only
- **Response**: `200 OK`

---

## 4. Project Phases

### 4.1 Create Phase
- **Endpoint**: `POST /api/projects/{projectCode}/phases`
- **Auth**: Builder owner only
- **Request Body**:
```json
{
  "name": "Foundation & Piling",
  "description": "Deep foundation piling and earth excavation",
  "sequenceOrder": 1,
  "startDate": "2026-12-01",
  "targetCompletionDate": "2027-03-31",
  "budget": 2500000.00
}
```
- **Response**: `201 Created`

### 4.2 List Phases
- **Endpoint**: `GET /api/projects/{projectCode}/phases`
- **Auth**: Read access
- **Response**: `200 OK`

### 4.3 Update Phase
- **Endpoint**: `PUT /api/projects/{projectCode}/phases/{phaseId}`
- **Auth**: Write access (Builder or Contractor)
- **Request Body**:
```json
{
  "status": "IN_PROGRESS",
  "sequenceOrder": 1
}
```
- **Response**: `200 OK`

---

## 5. Progress Updates

### 5.1 Submit Daily Construction Progress Update
- **Endpoint**: `POST /api/projects/{projectCode}/progress`
- **Auth**: Write access (Contractor or Builder)
- **Description**: Records daily construction progress, including manual area measurements and work category. Historical reports are preserved and never overwritten.
- **Request Body**:
```json
{
  "phaseId": "b1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "workCategory": "BRICKWORK",
  "areaCompleted": 250.00,
  "areaUnit": "SQFT",
  "percentageCompleted": 38.5,
  "title": "First floor brickwork progress",
  "description": "Completed installation of red brick masonry on northern perimeter.",
  "workCompletedToday": "250 SQFT of brickwork completed today.",
  "blockersDelays": "Groundwater ingress observed near Grid 9; water pump deployed.",
  "hasBlockers": true,
  "weatherNotes": "Clear skies, 22C",
  "updateDate": "2026-11-19"
}
```
- **Allowed Units**: `SQFT`, `SQM`, `CFT`, `RFT`, `UNITS`
- **Response**: `201 Created`
```json
{
  "success": true,
  "message": "Progress update recorded successfully",
  "data": {
    "id": "e4f5a6b7-c8d9-0123-ef45-678901234567",
    "phaseId": "b1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "phaseName": "Phase 2 - Masonry",
    "submittedById": "c39988fd-5ca8-471d-906c-1be9d8ad3d5c",
    "submittedByName": "Bob Contractor",
    "submittedByEmail": "contractor@apex.com",
    "workCategory": "BRICKWORK",
    "areaCompleted": 250.00,
    "areaUnit": "SQFT",
    "percentageCompleted": 38.50,
    "title": "First floor brickwork progress",
    "description": "Completed installation of red brick masonry on northern perimeter.",
    "workCompletedToday": "250 SQFT of brickwork completed today.",
    "blockersDelays": "Groundwater ingress observed near Grid 9; water pump deployed.",
    "hasBlockers": true,
    "weatherNotes": "Clear skies, 22C",
    "updateDate": "2026-11-19",
    "createdAt": "2026-11-19T13:00:00"
  },
  "timestamp": "2026-11-19T13:00:00"
}
```

### 5.2 List Progress Updates (Paginated & Date-Filtered)
- **Endpoint**: `GET /api/projects/{projectCode}/progress`
- **Query Parameters**:
  - `date`: Optional specific date (e.g. `2026-11-19`)
  - `startDate`: Optional range start (e.g. `2026-11-01`)
  - `endDate`: Optional range end (e.g. `2026-11-19`)
  - `page`: Page index (default `0`)
  - `size`: Page size (default `20`)
- **Auth**: Read access (Builder, Contractor, Client)
- **Response**: `200 OK`

### 5.3 Get Progress by Specific Date
- **Endpoint**: `GET /api/projects/{projectCode}/progress/date/{date}`
- **Auth**: Read access
- **Response**: `200 OK` (list of progress reports for that specific calendar date)

### 5.4 Get Latest Progress
- **Endpoint**: `GET /api/projects/{projectCode}/progress/latest`
- **Auth**: Read access
- **Response**: `200 OK`

---

## 6. Media & Documents (Supabase Storage)

### 6.1 Upload Media File or Site Photograph
- **Endpoint**: `POST /api/projects/{projectCode}/media`
- **Content-Type**: `multipart/form-data`
- **Auth**: Write access (Contractor or Builder)
- **Parameters**:
  - `file`: Multipart file (JPG, PNG, WEBP, PDF, DOCX, XLSX; max 25MB)
  - `category`: `SITE_PHOTO`, `BLUEPRINT`, `INVOICE`, `DOCUMENT`, `OTHER`
  - `caption`: Optional description
  - `progressUpdateId`: Optional UUID of daily progress report to link to
- **Response**: `201 Created`
```json
{
  "success": true,
  "message": "Media file uploaded successfully",
  "data": {
    "id": "7890abcd-ef12-3456-7890-abcdef123456",
    "progressUpdateId": "e4f5a6b7-c8d9-0123-ef45-678901234567",
    "fileName": "brickwork_photo.jpg",
    "storagePath": "PRJ-9X8K2M/7890abcd-brickwork_photo.jpg",
    "signedUrl": "https://xyz.supabase.co/storage/v1/object/sign/construction-media/PRJ-9X8K2M/7890abcd-brickwork_photo.jpg?token=...",
    "fileSize": 1048576,
    "mimeType": "image/jpeg",
    "mediaCategory": "SITE_PHOTO",
    "caption": "North wall brickwork",
    "uploadedById": "c39988fd-5ca8-471d-906c-1be9d8ad3d5c",
    "uploadedByName": "Bob Contractor",
    "createdAt": "2026-11-19T13:05:00"
  },
  "timestamp": "2026-11-19T13:05:00"
}
```

### 6.2 Trigger AI Site Photograph Analysis
- **Endpoint**: `POST /api/projects/{projectCode}/media/{mediaId}/analyze`
- **Query Parameters**: `reanalyze` (boolean, optional, default: `false`). Set to `true` to bypass cached results and force fresh inference.
- **Auth**: Write access (Contractor or Builder)
- **Description**: Triggers automated AI visual inspection of uploaded site photograph. If already analyzed, returns cached `COMPLETED` result immediately (idempotent). Persists analysis record in `media_ai_analyses` through workflow states: `PENDING` -> `PROCESSING` -> `COMPLETED` / `FAILED`.
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Media file AI analysis processed",
  "data": {
    "id": "12345678-abcd-ef01-2345-67890abcdef1",
    "mediaFileId": "7890abcd-ef12-3456-7890-abcdef123456",
    "mediaFileName": "foundation_photo.jpg",
    "mediaSignedUrl": "https://xyz.supabase.co/storage/v1/object/sign/...",
    "projectId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "projectCode": "PRJ-9X8K2M",
    "analyzedById": "c39988fd-5ca8-471d-906c-1be9d8ad3d5c",
    "analyzedByName": "Bob Contractor",
    "status": "COMPLETED",
    "aiProvider": "YOLO11_FASTAPI",
    "aiModel": "yolo11x-construction:1.0",
    "analysisSummary": "Foundation rebar reinforcement and timber formwork observed with high confidence.",
    "detectedWorkCategory": "FOUNDATION",
    "estimatedCompletionPercentage": 65.00,
    "detectedIssues": "[\"Water accumulation at trench perimeter\"]",
    "recommendations": "[\"Deploy dewatering pump prior to concrete pour\"]",
    "rawAnalysis": "{ ... }",
    "errorMessage": null,
    "createdAt": "2026-11-26T20:00:00",
    "completedAt": "2026-11-26T20:00:03"
  },
  "timestamp": "2026-11-26T20:00:03"
}
```

### 6.3 Get AI Analyses for Specific Photograph
- **Endpoint**: `GET /api/projects/{projectCode}/media/{mediaId}/analyses`
- **Auth**: Read access (Builder, Contractor, Client)
- **Response**: `200 OK` (list of historical analysis runs for this photo)

### 6.4 Get Latest AI Analysis for Specific Photograph
- **Endpoint**: `GET /api/projects/{projectCode}/media/{mediaId}/analyses/latest`
- **Auth**: Read access (Builder, Contractor, Client)
- **Description**: Returns the most recent AI analysis record for rapid frontend polling and status display.
- **Response**: `200 OK` (single `MediaAiAnalysisResponse`)

### 6.4 List Media Files
- **Endpoint**: `GET /api/projects/{projectCode}/media?category=SITE_PHOTO`
- **Auth**: Read access
- **Response**: `200 OK` (includes freshly generated signed URLs)

### 6.5 Delete Media File
- **Endpoint**: `DELETE /api/projects/{projectCode}/media/{mediaId}`
- **Auth**: Builder owner or original uploader only
- **Response**: `200 OK`

---

## 7. Materials & Inventory

### 7.1 Create Material Item
- **Endpoint**: `POST /api/projects/{projectCode}/materials`
- **Auth**: Write access
- **Request Body**:
```json
{
  "itemCode": "TMT-16MM",
  "name": "16mm TMT Steel Rebars Fe550D",
  "category": "Steel",
  "unitOfMeasure": "TONNES",
  "minimumThreshold": 10.0
}
```
- **Response**: `201 Created`

### 7.2 List Material Items
- **Endpoint**: `GET /api/projects/{projectCode}/materials`
- **Auth**: Read access
- **Response**: `200 OK`

### 7.3 Record Material Transaction
- **Endpoint**: `POST /api/projects/{projectCode}/materials/transactions`
- **Auth**: Write access (Contractor or Builder)
- **Request Body**:
```json
{
  "itemCode": "TMT-16MM",
  "transactionType": "USED",
  "quantity": 4.5,
  "referenceNumber": "SITE-REQ-104",
  "notes": "Used in pile cap reinforcement on Grid 4"
}
```
- **Response**: `201 Created`

### 7.4 Get Inventory Levels
- **Endpoint**: `GET /api/projects/{projectCode}/inventory`
- **Auth**: Read access
- **Response**: `200 OK` (includes current stock, reserved stock, and `isLowStock` indicator)

---

## 8. Budgets & Expenses

### 8.1 Create Budget Line
- **Endpoint**: `POST /api/projects/{projectCode}/budgets`
- **Auth**: Builder owner only
- **Request Body**:
```json
{
  "category": "MATERIALS",
  "plannedAmount": 12000000.00,
  "description": "Steel, cement, sand, aggregates, and bricks"
}
```
- **Response**: `201 Created`

### 8.2 List Budget Lines
- **Endpoint**: `GET /api/projects/{projectCode}/budgets`
- **Auth**: Read access
- **Response**: `200 OK`

### 8.3 Record Expense
- **Endpoint**: `POST /api/projects/{projectCode}/expenses`
- **Auth**: Write access (Contractor or Builder)
- **Request Body**:
```json
{
  "amount": 245000.00,
  "expenseDate": "2026-12-12",
  "category": "MATERIALS",
  "vendor": "Tata Steel Distribution Ltd",
  "invoiceNumber": "TATA-2026-8819",
  "description": "35 tonnes of 16mm TMT rebars"
}
```
- **Response**: `201 Created`

### 8.4 List Expenses
- **Endpoint**: `GET /api/projects/{projectCode}/expenses?page=0&size=20`
- **Auth**: Read access
- **Response**: `200 OK`

### 8.5 Financial Summary
- **Endpoint**: `GET /api/projects/{projectCode}/financial-summary`
- **Auth**: Read access
- **Description**: Calculates total planned budget, total actual expenses, remaining budget, variance, percentage spent, and breakdown by category.
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "projectCode": "PRJ-9X8K2M",
    "totalPlannedBudget": 12000000.00,
    "totalActualExpenses": 245000.00,
    "remainingBudget": 11755000.00,
    "budgetVariance": 11755000.00,
    "percentageSpent": 2.0417,
    "expensesByCategory": {
      "MATERIALS": 245000.00
    },
    "budgetByCategory": {
      "MATERIALS": 12000000.00
    }
  }
}
```

---

## 9. Labour Records

### 9.1 Log Labour
- **Endpoint**: `POST /api/projects/{projectCode}/labour`
- **Auth**: Write access (Contractor or Builder)
- **Request Body**:
```json
{
  "workDate": "2026-12-10",
  "tradeType": "MASON",
  "headCount": 8,
  "hoursWorked": 8.0,
  "hourlyRate": 65.0,
  "remarks": "Block A foundation curing and brick boundary"
}
```
- **Response**: `201 Created`

### 9.2 List Labour Records
- **Endpoint**: `GET /api/projects/{projectCode}/labour?page=0&size=20`
- **Auth**: Read access
- **Response**: `200 OK`

---

## 10. AI Insights

### 10.1 Project Risks
- **Endpoint**: `GET /api/projects/{projectCode}/ai/risks`
- **Auth**: Read access
- **Description**: Returns predictive risks and explainable AI feature weights. Returns honest `UNAVAILABLE_NO_PROVIDER_CONFIGURED` if AI provider is not enabled in environment.
- **Response**: `200 OK`

### 10.2 Project Recommendations
- **Endpoint**: `GET /api/projects/{projectCode}/ai/recommendations`
- **Auth**: Read access
- **Response**: `200 OK`

### 10.3 Productivity & EVM Metrics
- **Endpoint**: `GET /api/projects/{projectCode}/ai/productivity`
- **Auth**: Read access
- **Response**: `200 OK`

### 10.4 Project Site-Photo AI Analyses History
- **Endpoint**: `GET /api/projects/{projectCode}/ai/analyses`
- **Auth**: Read access (Builder, Contractor, Client)
- **Description**: Retrieves all persistent AI image inspection runs performed across all site photographs in the project. Returns independent AI work categories and completion percentage estimates without modifying or overriding manual contractor reports.
- **Response**: `200 OK` (list of `MediaAiAnalysisResponse` records with active signed URLs)

### 10.5 Retrieve Specific AI Analysis by ID
- **Endpoint**: `GET /api/projects/{projectCode}/ai/analyses/{analysisId}`
- **Auth**: Read access (Builder, Contractor, Client)
- **Description**: Retrieves a single AI analysis record by its unique UUID within the project.
- **Response**: `200 OK` (`MediaAiAnalysisResponse` record)

---

## 11. Notifications

### 11.1 List User Notifications
- **Endpoint**: `GET /api/notifications?page=0&size=20`
- **Auth**: Authenticated User
- **Response**: `200 OK`

### 11.2 Mark Notification as Read
- **Endpoint**: `PATCH /api/notifications/{notificationId}/read`
- **Auth**: Authenticated Recipient
- **Response**: `200 OK`
