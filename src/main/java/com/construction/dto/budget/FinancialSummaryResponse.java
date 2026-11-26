package com.construction.dto.budget;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialSummaryResponse {
    private String projectCode;
    private BigDecimal totalPlannedBudget;
    private BigDecimal totalActualExpenses;
    private BigDecimal remainingBudget;
    private BigDecimal budgetVariance;
    private BigDecimal percentageSpent;
    private Map<String, BigDecimal> expensesByCategory;
    private Map<String, BigDecimal> budgetByCategory;
}
