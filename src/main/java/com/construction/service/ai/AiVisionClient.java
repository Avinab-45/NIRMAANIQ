package com.construction.service.ai;

import java.math.BigDecimal;

public interface AiVisionClient {

    record VisionAnalysisResult(
            String provider,
            String model,
            String workCategory,
            BigDecimal estimatedCompletionPercentage,
            String analysisSummary,
            String detectedIssuesJson,
            String recommendationsJson,
            String rawAnalysisJson
    ) {}

    VisionAnalysisResult analyzeConstructionImage(byte[] imageBytes, String mimeType, String contextPrompt);
}
