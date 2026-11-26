package com.construction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "explanation_factors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExplanationFactor {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_prediction_id", nullable = false)
    private RiskPrediction riskPrediction;

    @Column(name = "factor_name", nullable = false)
    private String factorName;

    @Column(name = "importance_weight", precision = 5, scale = 2)
    private BigDecimal importanceWeight;

    @Column(name = "explanation_text", columnDefinition = "TEXT", nullable = false)
    private String explanationText;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
