package com.construction.dto.phase;

import com.construction.entity.enums.PhaseStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePhaseRequest {
    private String name;
    private String description;
    private Integer sequenceOrder;
    private PhaseStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate targetCompletionDate;
    private BigDecimal budget;
}
