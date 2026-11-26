package com.construction.entity;

import com.construction.entity.enums.WeatherImpact;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "weather_context")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherContext {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "recorded_date", nullable = false)
    private LocalDate recordedDate;

    @Column(name = "condition")
    private String condition;

    @Column(name = "temperature", precision = 5, scale = 2)
    private BigDecimal temperature;

    @Column(name = "humidity", precision = 5, scale = 2)
    private BigDecimal humidity;

    @Column(name = "rainfall_mm", precision = 6, scale = 2)
    private BigDecimal rainfallMm;

    @Enumerated(EnumType.STRING)
    @Column(name = "impact_on_work", nullable = false)
    private WeatherImpact impactOnWork;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.recordedAt == null) {
            this.recordedAt = LocalDateTime.now();
        }
        if (this.impactOnWork == null) {
            this.impactOnWork = WeatherImpact.NONE;
        }
    }
}
