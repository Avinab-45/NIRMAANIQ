package com.construction.dto.material;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLevelResponse {
    private UUID id;
    private UUID materialItemId;
    private String itemCode;
    private String itemName;
    private String category;
    private String unitOfMeasure;
    private BigDecimal currentStock;
    private BigDecimal reservedStock;
    private BigDecimal minimumThreshold;
    private boolean isLowStock;
    private LocalDateTime lastUpdated;
}
