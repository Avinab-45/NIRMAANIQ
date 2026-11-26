package com.construction.dto.labour;

import com.construction.entity.enums.TradeType;
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
public class LabourRecordResponse {
    private UUID id;
    private UUID phaseId;
    private String phaseName;
    private LocalDate workDate;
    private TradeType tradeType;
    private int headCount;
    private BigDecimal hoursWorked;
    private BigDecimal hourlyRate;
    private BigDecimal totalCost;
    private String remarks;
    private UUID recordedById;
    private String recordedByName;
    private LocalDateTime createdAt;
}
