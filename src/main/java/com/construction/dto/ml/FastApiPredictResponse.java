package com.construction.dto.ml;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Flexible DTO mapping YOLO11 FastAPI inference responses.
 * Uses @JsonAlias to adapt to the ML teammate's finalized field naming conventions.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FastApiPredictResponse {

    @JsonAlias({"model", "model_name", "model_id", "name"})
    private String model;

    @JsonAlias({"version", "model_version", "checkpoint"})
    private String version;

    @JsonAlias({"status", "state", "result_status"})
    private String status;

    @JsonAlias({"summary", "analysis_summary", "description", "message", "text"})
    private String summary;

    @JsonAlias({"work_category", "workCategory", "category", "stage", "construction_stage", "class"})
    private String workCategory;

    @JsonAlias({"estimated_completion_percentage", "completion_percentage", "progress_percentage", "percentage", "progress", "estimated_progress"})
    private BigDecimal estimatedCompletionPercentage;

    @JsonAlias({"detections", "predictions", "objects", "boxes", "results", "data", "items"})
    private List<MlDetection> detections;

    @JsonAlias({"issues", "detected_issues", "hazards", "violations", "defects", "warnings", "alerts"})
    private List<String> issues;

    @JsonAlias({"recommendations", "suggestions", "actions", "corrective_actions", "remedies"})
    private List<String> recommendations;

    @JsonAlias({"metadata", "extra", "info", "diagnostics", "stats"})
    private Map<String, Object> metadata;

    /**
     * Stored raw JSON response string for full auditability and retraining.
     */
    private String rawJson;
}
