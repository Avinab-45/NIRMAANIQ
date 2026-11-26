package com.construction.entity;

import com.construction.entity.enums.AiAnalysisStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "media_ai_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaAiAnalysis {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_file_id", nullable = false)
    private MediaFile mediaFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzed_by")
    private Profile analyzedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AiAnalysisStatus status;

    @Column(name = "ai_provider", length = 100)
    private String aiProvider;

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Column(name = "analysis_summary", columnDefinition = "TEXT")
    private String analysisSummary;

    @Column(name = "detected_work_category", length = 100)
    private String detectedWorkCategory;

    @Column(name = "estimated_completion_percentage", precision = 5, scale = 2)
    private BigDecimal estimatedCompletionPercentage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detected_issues", columnDefinition = "jsonb")
    private String detectedIssues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendations", columnDefinition = "jsonb")
    private String recommendations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_analysis", columnDefinition = "jsonb")
    private String rawAnalysis;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = AiAnalysisStatus.PENDING;
        }
    }
}
