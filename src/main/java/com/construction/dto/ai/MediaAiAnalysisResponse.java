package com.construction.dto.ai;

import com.construction.entity.enums.AiAnalysisStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaAiAnalysisResponse {
    private UUID id;
    private UUID mediaFileId;
    private String mediaFileName;
    private String mediaSignedUrl;
    private UUID projectId;
    private String projectCode;
    private UUID analyzedById;
    private String analyzedByName;
    private AiAnalysisStatus status;
    private String aiProvider;
    private String aiModel;
    private String analysisSummary;
    private String detectedWorkCategory;
    private BigDecimal estimatedCompletionPercentage;
    private String detectedIssues;
    private String recommendations;
    private String rawAnalysis;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
