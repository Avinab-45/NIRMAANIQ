package com.construction.entity;

import com.construction.entity.enums.RiskCategory;
import com.construction.entity.enums.RiskLevel;
import com.construction.entity.enums.RiskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "risk_predictions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskPrediction {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category", nullable = false)
    private RiskCategory riskCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(name = "probability", precision = 5, scale = 2)
    private BigDecimal probability;

    @Column(name = "impact_score", precision = 5, scale = 2)
    private BigDecimal impactScore;

    @Column(name = "prediction_summary", columnDefinition = "TEXT", nullable = false)
    private String predictionSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RiskStatus status;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @OneToMany(mappedBy = "riskPrediction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExplanationFactor> explanationFactors = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.generatedAt == null) {
            this.generatedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = RiskStatus.ACTIVE;
        }
    }
}
