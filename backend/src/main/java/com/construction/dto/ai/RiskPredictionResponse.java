package com.construction.dto.ai;

import com.construction.entity.enums.RiskCategory;
import com.construction.entity.enums.RiskLevel;
import com.construction.entity.enums.RiskStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskPredictionResponse {
    private UUID id;
    private RiskCategory riskCategory;
    private RiskLevel riskLevel;
    private BigDecimal probability;
    private BigDecimal impactScore;
    private String predictionSummary;
    private RiskStatus status;
    private LocalDateTime generatedAt;
    private List<ExplanationFactorResponse> explanationFactors;
}
