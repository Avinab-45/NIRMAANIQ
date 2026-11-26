package com.construction.dto.ai;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductivityMetricResponse {
    private UUID id;
    private LocalDate metricDate;
    private String metricName;
    private BigDecimal plannedValue;
    private BigDecimal earnedValue;
    private BigDecimal actualCost;
    private BigDecimal costPerformanceIndex;
    private BigDecimal schedulePerformanceIndex;
    private LocalDateTime calculatedAt;
}
