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
public class MaterialItemResponse {
    private UUID id;
    private String itemCode;
    private String name;
    private String category;
    private String unitOfMeasure;
    private BigDecimal minimumThreshold;
    private BigDecimal currentStock;
    private boolean isLowStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
