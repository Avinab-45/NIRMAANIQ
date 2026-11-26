package com.construction.dto.progress;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProgressRequest {

    private UUID phaseId;

    @Size(max = 100, message = "Work category cannot exceed 100 characters")
    private String workCategory;

    @DecimalMin(value = "0.0", message = "Area completed cannot be negative")
    private BigDecimal areaCompleted;

    @Size(max = 50, message = "Area unit cannot exceed 50 characters")
    private String areaUnit;

    @NotNull(message = "Percentage completed is required")
    @DecimalMin(value = "0.0", message = "Percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Percentage cannot exceed 100%")
    private BigDecimal percentageCompleted;

    @NotBlank(message = "Progress title is required")
    private String title;

    private String description;
    private String workCompletedToday;
    private String blockersDelays;
    private boolean hasBlockers;
    private String weatherNotes;
    private LocalDate updateDate;
}
