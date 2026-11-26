package com.construction.entity;

import com.construction.entity.enums.Priority;
import com.construction.entity.enums.RecommendationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_prediction_id")
    private RiskPrediction riskPrediction;

    @Column(name = "category")
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private Priority priority;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "action_steps", columnDefinition = "TEXT", nullable = false)
    private String actionSteps;

    @Column(name = "expected_benefit", columnDefinition = "TEXT")
    private String expectedBenefit;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RecommendationStatus status;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.generatedAt == null) {
            this.generatedAt = LocalDateTime.now();
        }
        if (this.priority == null) {
            this.priority = Priority.MEDIUM;
        }
        if (this.status == null) {
            this.status = RecommendationStatus.PENDING;
        }
    }
}
