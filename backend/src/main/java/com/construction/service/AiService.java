package com.construction.service;

import com.construction.dto.ai.*;
import com.construction.entity.ExplanationFactor;
import com.construction.entity.ProductivityMetric;
import com.construction.entity.Recommendation;
import com.construction.entity.RiskPrediction;
import com.construction.repository.ExplanationFactorRepository;
import com.construction.repository.ProductivityMetricRepository;
import com.construction.repository.RecommendationRepository;
import com.construction.repository.RiskPredictionRepository;
import com.construction.security.ProjectAccessService;
import com.construction.service.ml.MlAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final RiskPredictionRepository riskPredictionRepository;
    private final ExplanationFactorRepository explanationFactorRepository;
    private final RecommendationRepository recommendationRepository;
    private final ProductivityMetricRepository productivityMetricRepository;
    private final ProjectAccessService projectAccessService;
    private final MlAnalysisService mlAnalysisService;

    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${app.ai.api-key:}")
    private String aiApiKey;

    /**
     * Executes AI site photograph visual analysis using the primary YOLO11 FastAPI ML microservice.
     * Enforces idempotency unless explicitly forced to reanalyze.
     */
    @Transactional
    public MediaAiAnalysisResponse analyzeMediaFile(String projectCode, UUID mediaId) {
        return mlAnalysisService.analyzeMediaFile(projectCode, mediaId, false);
    }

    /**
     * Executes AI site photograph visual analysis with optional force-reanalyze override.
     */
    @Transactional
    public MediaAiAnalysisResponse analyzeMediaFile(String projectCode, UUID mediaId, boolean reanalyze) {
        return mlAnalysisService.analyzeMediaFile(projectCode, mediaId, reanalyze);
    }

    /**
     * Retrieves the latest AI analysis for a specific photograph.
     */
    @Transactional(readOnly = true)
    public MediaAiAnalysisResponse getLatestMediaAnalysis(String projectCode, UUID mediaId) {
        return mlAnalysisService.getLatestAnalysis(projectCode, mediaId);
    }

    /**
     * Retrieves all persistent AI analyses for a specific photograph.
     */
    @Transactional(readOnly = true)
    public List<MediaAiAnalysisResponse> getMediaAnalyses(String projectCode, UUID mediaId) {
        return mlAnalysisService.getMediaAnalyses(projectCode, mediaId);
    }

    /**
     * Retrieves all persistent AI analyses across the entire project for Builder and Client reporting.
     */
    @Transactional(readOnly = true)
    public List<MediaAiAnalysisResponse> getProjectAiAnalyses(String projectCode) {
        return mlAnalysisService.getProjectAnalyses(projectCode);
    }

    /**
     * Retrieves a single AI analysis by analysis UUID.
     */
    @Transactional(readOnly = true)
    public MediaAiAnalysisResponse getAiAnalysisById(String projectCode, UUID analysisId) {
        return mlAnalysisService.getAnalysisById(projectCode, analysisId);
    }

    /**
     * Retrieves AI-calculated risk predictions and explanation factors.
     * Returns an honest status if no AI provider is configured or no predictions generated.
     */
    @Transactional(readOnly = true)
    public AiInsightStatusResponse<List<RiskPredictionResponse>> getRiskPredictions(String projectCode) {
        projectAccessService.validateProjectReadAccess(projectCode);

        List<RiskPrediction> predictions = riskPredictionRepository.findByProject_ProjectCodeOrderByGeneratedAtDesc(projectCode);

        if (!predictions.isEmpty()) {
            List<RiskPredictionResponse> responses = predictions.stream()
                    .map(this::mapToRiskPredictionResponse)
                    .toList();
            return AiInsightStatusResponse.available(responses, "Project risk predictions retrieved successfully.");
        }

        if (!aiEnabled || aiApiKey == null || aiApiKey.isBlank()) {
            return AiInsightStatusResponse.unavailable(
                    "AI predictive engine is currently not configured. Set AI_PROVIDER_ENABLED=true and configure AI_PROVIDER_API_KEY in your environment."
            );
        }

        return AiInsightStatusResponse.notYetGenerated(
            "AI provider is configured, but no risk predictions have been generated yet for project " + projectCode + "."
        );
    }

    /**
     * Retrieves AI recommendations for project mitigation.
     */
    @Transactional(readOnly = true)
    public AiInsightStatusResponse<List<RecommendationResponse>> getRecommendations(String projectCode) {
        projectAccessService.validateProjectReadAccess(projectCode);

        List<Recommendation> recommendations = recommendationRepository.findByProject_ProjectCodeOrderByGeneratedAtDesc(projectCode);

        if (!recommendations.isEmpty()) {
            List<RecommendationResponse> responses = recommendations.stream()
                    .map(this::mapToRecommendationResponse)
                    .toList();
            return AiInsightStatusResponse.available(responses, "Project AI recommendations retrieved successfully.");
        }

        if (!aiEnabled || aiApiKey == null || aiApiKey.isBlank()) {
            return AiInsightStatusResponse.unavailable(
                    "AI recommendation engine is currently not configured. Set AI_PROVIDER_ENABLED=true and configure AI_PROVIDER_API_KEY in your environment."
            );
        }

        return AiInsightStatusResponse.notYetGenerated(
                "AI provider is configured, but no recommendations have been generated yet for project " + projectCode + "."
        );
    }

    /**
     * Retrieves productivity and earned-value metrics.
     */
    @Transactional(readOnly = true)
    public AiInsightStatusResponse<List<ProductivityMetricResponse>> getProductivityMetrics(String projectCode) {
        projectAccessService.validateProjectReadAccess(projectCode);

        List<ProductivityMetric> metrics = productivityMetricRepository.findByProject_ProjectCodeOrderByMetricDateDesc(projectCode);

        if (!metrics.isEmpty()) {
            List<ProductivityMetricResponse> responses = metrics.stream()
                    .map(this::mapToProductivityMetricResponse)
                    .toList();
            return AiInsightStatusResponse.available(responses, "Project productivity metrics retrieved successfully.");
        }

        if (!aiEnabled || aiApiKey == null || aiApiKey.isBlank()) {
            return AiInsightStatusResponse.unavailable(
                    "AI productivity calculation service is currently not configured. Set AI_PROVIDER_ENABLED=true and configure AI_PROVIDER_API_KEY in your environment."
            );
        }

        return AiInsightStatusResponse.notYetGenerated(
                "No productivity metrics have been calculated yet for project " + projectCode + "."
        );
    }

    private RiskPredictionResponse mapToRiskPredictionResponse(RiskPrediction rp) {
        List<ExplanationFactorResponse> factors = explanationFactorRepository.findByRiskPrediction_Id(rp.getId())
                .stream()
                .map(ef -> ExplanationFactorResponse.builder()
                        .id(ef.getId())
                        .factorName(ef.getFactorName())
                        .importanceWeight(ef.getImportanceWeight())
                        .explanationText(ef.getExplanationText())
                        .build())
                .toList();

        return RiskPredictionResponse.builder()
                .id(rp.getId())
                .riskCategory(rp.getRiskCategory())
                .riskLevel(rp.getRiskLevel())
                .probability(rp.getProbability())
                .impactScore(rp.getImpactScore())
                .predictionSummary(rp.getPredictionSummary())
                .status(rp.getStatus())
                .generatedAt(rp.getGeneratedAt())
                .explanationFactors(factors)
                .build();
    }

    private RecommendationResponse mapToRecommendationResponse(Recommendation r) {
        return RecommendationResponse.builder()
                .id(r.getId())
                .riskPredictionId(r.getRiskPrediction() != null ? r.getRiskPrediction().getId() : null)
                .category(r.getCategory())
                .priority(r.getPriority())
                .title(r.getTitle())
                .actionSteps(r.getActionSteps())
                .expectedBenefit(r.getExpectedBenefit())
                .status(r.getStatus())
                .generatedAt(r.getGeneratedAt())
                .build();
    }

    private ProductivityMetricResponse mapToProductivityMetricResponse(ProductivityMetric pm) {
        return ProductivityMetricResponse.builder()
                .id(pm.getId())
                .metricDate(pm.getMetricDate())
                .metricName(pm.getMetricName())
                .plannedValue(pm.getPlannedValue())
                .earnedValue(pm.getEarnedValue())
                .actualCost(pm.getActualCost())
                .costPerformanceIndex(pm.getCostPerformanceIndex())
                .schedulePerformanceIndex(pm.getSchedulePerformanceIndex())
                .calculatedAt(pm.getCalculatedAt())
                .build();
    }
}
