package com.construction.dto.material;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMaterialItemRequest {

    @NotBlank(message = "Item code is required")
    private String itemCode;

    @NotBlank(message = "Item name is required")
    private String name;

    private String category;

    @NotBlank(message = "Unit of measure is required (e.g., BAGS, KG, TONNES, LITRES, SQFT)")
    private String unitOfMeasure;

    @DecimalMin(value = "0.0", message = "Minimum threshold cannot be negative")
    private BigDecimal minimumThreshold;
}
