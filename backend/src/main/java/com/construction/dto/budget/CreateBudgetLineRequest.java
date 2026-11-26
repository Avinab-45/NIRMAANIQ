package com.construction.dto.budget;

import com.construction.entity.enums.CostCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBudgetLineRequest {

    private UUID phaseId;

    @NotNull(message = "Category is required")
    private CostCategory category;

    @NotNull(message = "Planned amount is required")
    @DecimalMin(value = "0.01", message = "Planned amount must be greater than zero")
    private BigDecimal plannedAmount;

    private String description;
}
