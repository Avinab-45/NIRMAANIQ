package com.construction.dto.phase;

import com.construction.entity.enums.PhaseStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePhaseRequest {

    @NotBlank(message = "Phase name is required")
    private String name;

    private String description;

    @Min(value = 1, message = "Sequence order must be at least 1")
    private int sequenceOrder;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate targetCompletionDate;
    private BigDecimal budget;
}
