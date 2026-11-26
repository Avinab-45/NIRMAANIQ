package com.construction.dto.phase;

import com.construction.entity.enums.PhaseStatus;
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
public class PhaseResponse {
    private UUID id;
    private String name;
    private String description;
    private int sequenceOrder;
    private PhaseStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate targetCompletionDate;
    private BigDecimal budget;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
