package com.construction.controller;

import com.construction.dto.ai.*;
import com.construction.dto.common.ApiResponse;
import com.construction.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectCode}/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @GetMapping("/risks")
    public ResponseEntity<AiInsightStatusResponse<List<RiskPredictionResponse>>> getRisks(
            @PathVariable String projectCode) {
        AiInsightStatusResponse<List<RiskPredictionResponse>> response = aiService.getRiskPredictions(projectCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<AiInsightStatusResponse<List<RecommendationResponse>>> getRecommendations(
            @PathVariable String projectCode) {
        AiInsightStatusResponse<List<RecommendationResponse>> response = aiService.getRecommendations(projectCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/productivity")
    public ResponseEntity<AiInsightStatusResponse<List<ProductivityMetricResponse>>> getProductivity(
            @PathVariable String projectCode) {
        AiInsightStatusResponse<List<ProductivityMetricResponse>> response = aiService.getProductivityMetrics(projectCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analyses")
    public ResponseEntity<ApiResponse<List<MediaAiAnalysisResponse>>> getProjectAiAnalyses(
            @PathVariable String projectCode) {
        List<MediaAiAnalysisResponse> analyses = aiService.getProjectAiAnalyses(projectCode);
        return ResponseEntity.ok(ApiResponse.success(analyses));
    }

    @GetMapping("/analyses/{analysisId}")
    public ResponseEntity<ApiResponse<MediaAiAnalysisResponse>> getAiAnalysisById(
            @PathVariable String projectCode,
            @PathVariable UUID analysisId) {
        MediaAiAnalysisResponse analysis = aiService.getAiAnalysisById(projectCode, analysisId);
        return ResponseEntity.ok(ApiResponse.success(analysis));
    }
}
