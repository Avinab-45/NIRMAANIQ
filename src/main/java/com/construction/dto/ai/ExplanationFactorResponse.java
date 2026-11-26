package com.construction.dto.ai;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExplanationFactorResponse {
    private UUID id;
    private String factorName;
    private BigDecimal importanceWeight;
    private String explanationText;
}
