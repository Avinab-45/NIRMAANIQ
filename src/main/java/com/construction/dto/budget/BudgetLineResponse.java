package com.construction.dto.budget;

import com.construction.entity.enums.CostCategory;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetLineResponse {
    private UUID id;
    private UUID phaseId;
    private String phaseName;
    private CostCategory category;
    private BigDecimal plannedAmount;
    private BigDecimal actualAmount;
    private BigDecimal remainingAmount;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
