package com.construction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_levels", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"project_id", "material_item_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLevel {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_item_id", nullable = false)
    private MaterialItem materialItem;

    @Column(name = "current_stock", precision = 12, scale = 2, nullable = false)
    private BigDecimal currentStock;

    @Column(name = "reserved_stock", precision = 12, scale = 2)
    private BigDecimal reservedStock;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.currentStock == null) {
            this.currentStock = BigDecimal.ZERO;
        }
        if (this.reservedStock == null) {
            this.reservedStock = BigDecimal.ZERO;
        }
        this.lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }
}
