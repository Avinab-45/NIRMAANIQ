package com.construction.dto.progress;

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
public class ProgressResponse {
    private UUID id;
    private UUID phaseId;
    private String phaseName;
    private UUID submittedById;
    private String submittedByName;
    private String submittedByEmail;
    private String workCategory;
    private BigDecimal areaCompleted;
    private String areaUnit;
    private BigDecimal percentageCompleted;
    private String title;
    private String description;
    private String workCompletedToday;
    private String blockersDelays;
    private boolean hasBlockers;
    private String weatherNotes;
    private LocalDate updateDate;
    private LocalDateTime createdAt;
}
