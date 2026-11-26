package com.construction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "progress_updates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressUpdate {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id")
    private ProjectPhase phase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by", nullable = false)
    private Profile submittedBy;

    @Column(name = "percentage_completed", precision = 5, scale = 2, nullable = false)
    private BigDecimal percentageCompleted;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "work_category", length = 100)
    private String workCategory;

    @Column(name = "area_completed", precision = 12, scale = 2)
    private BigDecimal areaCompleted;

    @Column(name = "area_unit", length = 50)
    private String areaUnit;

    @Column(name = "work_completed_today", columnDefinition = "TEXT")
    private String workCompletedToday;

    @Column(name = "blockers_delays", columnDefinition = "TEXT")
    private String blockersDelays;

    @Column(name = "has_blockers", nullable = false)
    private boolean hasBlockers;

    @Column(name = "weather_notes")
    private String weatherNotes;

    @Column(name = "update_date", nullable = false)
    private LocalDate updateDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updateDate == null) {
            this.updateDate = LocalDate.now();
        }
    }
}
