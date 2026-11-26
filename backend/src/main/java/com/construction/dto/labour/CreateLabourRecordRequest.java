package com.construction.dto.labour;

import com.construction.entity.enums.TradeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLabourRecordRequest {

    private UUID phaseId;

    private LocalDate workDate;

    @NotNull(message = "Trade type is required (MASON, CARPENTER, ELECTRICIAN, PLUMBER, LABOURER, SUPERVISOR, OTHER)")
    private TradeType tradeType;

    @Min(value = 1, message = "Head count must be at least 1")
    private int headCount;

    @NotNull(message = "Hours worked is required")
    @DecimalMin(value = "0.5", message = "Hours worked must be at least 0.5")
    private BigDecimal hoursWorked;

    private BigDecimal hourlyRate;
    private BigDecimal totalCost;
    private String remarks;
}
