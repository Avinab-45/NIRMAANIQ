package com.construction.entity;

import com.construction.entity.enums.TradeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "labour_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabourRecord {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id")
    private ProjectPhase phase;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false)
    private TradeType tradeType;

    @Column(name = "head_count", nullable = false)
    private int headCount;

    @Column(name = "hours_worked", precision = 6, scale = 2, nullable = false)
    private BigDecimal hoursWorked;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "total_cost", precision = 14, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", nullable = false)
    private Profile recordedBy;

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
        if (this.workDate == null) {
            this.workDate = LocalDate.now();
        }
        if (this.totalCost == null && this.hourlyRate != null && this.hoursWorked != null) {
            BigDecimal headCountBd = BigDecimal.valueOf(Math.max(1, this.headCount));
            this.totalCost = this.hourlyRate.multiply(this.hoursWorked).multiply(headCountBd);
        }
    }
}
