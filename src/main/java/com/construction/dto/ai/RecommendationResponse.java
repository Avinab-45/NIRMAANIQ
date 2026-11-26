package com.construction.dto.ai;

import com.construction.entity.enums.Priority;
import com.construction.entity.enums.RecommendationStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {
    private UUID id;
    private UUID riskPredictionId;
    private String category;
    private Priority priority;
    private String title;
    private String actionSteps;
    private String expectedBenefit;
    private RecommendationStatus status;
    private LocalDateTime generatedAt;
}
