package com.construction.service;

import com.construction.dto.ai.*;
import com.construction.entity.ExplanationFactor;
import com.construction.entity.ProductivityMetric;
import com.construction.entity.Recommendation;
import com.construction.entity.RiskPrediction;
import com.construction.entity.enums.*;
import com.construction.repository.ExplanationFactorRepository;
import com.construction.repository.ProductivityMetricRepository;
import com.construction.repository.RecommendationRepository;
import com.construction.repository.RiskPredictionRepository;
import com.construction.security.ProjectAccessService;
import com.construction.service.ml.MlAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private RiskPredictionRepository riskPredictionRepository;

    @Mock
    private ExplanationFactorRepository explanationFactorRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private ProductivityMetricRepository productivityMetricRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private MlAnalysisService mlAnalysisService;

    @InjectMocks
    private AiService aiService;

    @Test
    @DisplayName("analyzeMediaFile delegates to mlAnalysisService with default reanalyze=false")
    void analyzeMediaFile_DelegatesToMlAnalysisService() {
        UUID mediaId = UUID.randomUUID();
        MediaAiAnalysisResponse mockResponse = MediaAiAnalysisResponse.builder()
                .id(UUID.randomUUID())
                .mediaFileId(mediaId)
                .status(AiAnalysisStatus.COMPLETED)
                .aiProvider("YOLO11")
                .aiModel("yolo11x-construction")
                .build();

        when(mlAnalysisService.analyzeMediaFile("PRJ-01", mediaId, false)).thenReturn(mockResponse);

        MediaAiAnalysisResponse response = aiService.analyzeMediaFile("PRJ-01", mediaId);

        assertNotNull(response);
        assertEquals(AiAnalysisStatus.COMPLETED, response.getStatus());
        assertEquals("YOLO11", response.getAiProvider());
        verify(mlAnalysisService).analyzeMediaFile("PRJ-01", mediaId, false);
    }

    @Test
    @DisplayName("analyzeMediaFile with explicit reanalyze flag delegates correctly")
    void analyzeMediaFile_WithReanalyzeFlag_DelegatesCorrectly() {
        UUID mediaId = UUID.randomUUID();
        MediaAiAnalysisResponse mockResponse = MediaAiAnalysisResponse.builder()
                .id(UUID.randomUUID())
                .mediaFileId(mediaId)
                .status(AiAnalysisStatus.COMPLETED)
                .build();

        when(mlAnalysisService.analyzeMediaFile("PRJ-01", mediaId, true)).thenReturn(mockResponse);

        MediaAiAnalysisResponse response = aiService.analyzeMediaFile("PRJ-01", mediaId, true);

        assertNotNull(response);
        verify(mlAnalysisService).analyzeMediaFile("PRJ-01", mediaId, true);
    }

    @Test
    @DisplayName("getLatestMediaAnalysis delegates to mlAnalysisService")
    void getLatestMediaAnalysis_DelegatesToMlAnalysisService() {
        UUID mediaId = UUID.randomUUID();
        MediaAiAnalysisResponse mockResponse = MediaAiAnalysisResponse.builder()
                .id(UUID.randomUUID())
                .mediaFileId(mediaId)
                .status(AiAnalysisStatus.COMPLETED)
                .build();

        when(mlAnalysisService.getLatestAnalysis("PRJ-01", mediaId)).thenReturn(mockResponse);

        MediaAiAnalysisResponse response = aiService.getLatestMediaAnalysis("PRJ-01", mediaId);

        assertNotNull(response);
        verify(mlAnalysisService).getLatestAnalysis("PRJ-01", mediaId);
    }

    @Test
    @DisplayName("getMediaAnalyses delegates to mlAnalysisService")
    void getMediaAnalyses_DelegatesToMlAnalysisService() {
        UUID mediaId = UUID.randomUUID();
        when(mlAnalysisService.getMediaAnalyses("PRJ-01", mediaId)).thenReturn(List.of());

        List<MediaAiAnalysisResponse> responses = aiService.getMediaAnalyses("PRJ-01", mediaId);

        assertNotNull(responses);
        verify(mlAnalysisService).getMediaAnalyses("PRJ-01", mediaId);
    }

    @Test
    @DisplayName("getProjectAiAnalyses delegates to mlAnalysisService")
    void getProjectAiAnalyses_DelegatesToMlAnalysisService() {
        when(mlAnalysisService.getProjectAnalyses("PRJ-01")).thenReturn(List.of());

        List<MediaAiAnalysisResponse> responses = aiService.getProjectAiAnalyses("PRJ-01");

        assertNotNull(responses);
        verify(mlAnalysisService).getProjectAnalyses("PRJ-01");
    }

    @Test
    @DisplayName("getAiAnalysisById delegates to mlAnalysisService")
    void getAiAnalysisById_DelegatesToMlAnalysisService() {
        UUID analysisId = UUID.randomUUID();
        MediaAiAnalysisResponse mockResponse = MediaAiAnalysisResponse.builder()
                .id(analysisId)
                .status(AiAnalysisStatus.COMPLETED)
                .build();

        when(mlAnalysisService.getAnalysisById("PRJ-01", analysisId)).thenReturn(mockResponse);

        MediaAiAnalysisResponse response = aiService.getAiAnalysisById("PRJ-01", analysisId);

        assertNotNull(response);
        assertEquals(analysisId, response.getId());
        verify(mlAnalysisService).getAnalysisById("PRJ-01", analysisId);
    }

    @Test
    @DisplayName("getRiskPredictions returns predictions when available")
    void getRiskPredictions_Available() {
        UUID riskId = UUID.randomUUID();
        RiskPrediction prediction = RiskPrediction.builder()
                .id(riskId)
                .riskCategory(RiskCategory.SCHEDULE)
                .riskLevel(RiskLevel.HIGH)
                .probability(new BigDecimal("0.75"))
                .impactScore(new BigDecimal("8.0"))
                .predictionSummary("Potential delay in foundation pouring due to weather")
                .status(RiskStatus.ACTIVE)
                .generatedAt(LocalDateTime.now())
                .build();

        ExplanationFactor factor = ExplanationFactor.builder()
                .id(UUID.randomUUID())
                .riskPrediction(prediction)
                .factorName("Precipitation Forecast")
                .importanceWeight(new BigDecimal("0.85"))
                .explanationText("Heavy monsoon rains expected this week")
                .build();

        when(riskPredictionRepository.findByProject_ProjectCodeOrderByGeneratedAtDesc("PRJ-01"))
                .thenReturn(List.of(prediction));
        when(explanationFactorRepository.findByRiskPrediction_Id(riskId))
                .thenReturn(List.of(factor));

        AiInsightStatusResponse<List<RiskPredictionResponse>> response = aiService.getRiskPredictions("PRJ-01");

        assertNotNull(response);
        assertEquals("AVAILABLE", response.getStatus());
        assertEquals(1, response.getInsights().size());
        assertEquals(RiskCategory.SCHEDULE, response.getInsights().get(0).getRiskCategory());
        assertEquals(1, response.getInsights().get(0).getExplanationFactors().size());
        verify(projectAccessService).validateProjectReadAccess("PRJ-01");
    }

    @Test
    @DisplayName("getRiskPredictions returns honest unavailable status when AI provider disabled")
    void getRiskPredictions_UnavailableWhenDisabled() {
        ReflectionTestUtils.setField(aiService, "aiEnabled", false);
        when(riskPredictionRepository.findByProject_ProjectCodeOrderByGeneratedAtDesc("PRJ-01"))
                .thenReturn(List.of());

        AiInsightStatusResponse<List<RiskPredictionResponse>> response = aiService.getRiskPredictions("PRJ-01");

        assertNotNull(response);
        assertEquals("UNAVAILABLE_NO_PROVIDER_CONFIGURED", response.getStatus());
        assertTrue(response.getMessage().contains("currently not configured"));
    }

    @Test
    @DisplayName("getRecommendations returns recommendations when available")
    void getRecommendations_Available() {
        Recommendation recommendation = Recommendation.builder()
                .id(UUID.randomUUID())
                .category("SCHEDULE")
                .priority(Priority.HIGH)
                .title("Add second concrete pump crew")
                .actionSteps("Procure rental pump and schedule overtime shift")
                .expectedBenefit("Mitigate 3 days of delay")
                .status(RecommendationStatus.PENDING)
                .generatedAt(LocalDateTime.now())
                .build();

        when(recommendationRepository.findByProject_ProjectCodeOrderByGeneratedAtDesc("PRJ-01"))
                .thenReturn(List.of(recommendation));

        AiInsightStatusResponse<List<RecommendationResponse>> response = aiService.getRecommendations("PRJ-01");

        assertNotNull(response);
        assertEquals("AVAILABLE", response.getStatus());
        assertEquals(1, response.getInsights().size());
        assertEquals("Add second concrete pump crew", response.getInsights().get(0).getTitle());
    }

    @Test
    @DisplayName("getProductivityMetrics returns metrics when available")
    void getProductivityMetrics_Available() {
        ProductivityMetric metric = ProductivityMetric.builder()
                .id(UUID.randomUUID())
                .metricDate(LocalDate.now())
                .metricName("Foundation Concrete Pour")
                .plannedValue(new BigDecimal("10000.00"))
                .earnedValue(new BigDecimal("9500.00"))
                .actualCost(new BigDecimal("9200.00"))
                .costPerformanceIndex(new BigDecimal("1.03"))
                .schedulePerformanceIndex(new BigDecimal("0.95"))
                .calculatedAt(LocalDateTime.now())
                .build();

        when(productivityMetricRepository.findByProject_ProjectCodeOrderByMetricDateDesc("PRJ-01"))
                .thenReturn(List.of(metric));

        AiInsightStatusResponse<List<ProductivityMetricResponse>> response = aiService.getProductivityMetrics("PRJ-01");

        assertNotNull(response);
        assertEquals("AVAILABLE", response.getStatus());
        assertEquals(1, response.getInsights().size());
        assertEquals("Foundation Concrete Pour", response.getInsights().get(0).getMetricName());
    }
}
