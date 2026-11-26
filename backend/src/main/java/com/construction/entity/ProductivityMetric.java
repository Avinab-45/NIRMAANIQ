package com.construction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "productivity_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductivityMetric {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column(name = "planned_value", precision = 14, scale = 2)
    private BigDecimal plannedValue;

    @Column(name = "earned_value", precision = 14, scale = 2)
    private BigDecimal earnedValue;

    @Column(name = "actual_cost", precision = 14, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "cost_performance_index", precision = 6, scale = 3)
    private BigDecimal costPerformanceIndex;

    @Column(name = "schedule_performance_index", precision = 6, scale = 3)
    private BigDecimal schedulePerformanceIndex;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.calculatedAt == null) {
            this.calculatedAt = LocalDateTime.now();
        }
    }
}
