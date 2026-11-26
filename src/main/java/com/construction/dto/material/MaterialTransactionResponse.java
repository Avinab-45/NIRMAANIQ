package com.construction.dto.material;

import com.construction.entity.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialTransactionResponse {
    private UUID id;
    private String itemCode;
    private String itemName;
    private TransactionType transactionType;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalCost;
    private String referenceNumber;
    private String notes;
    private UUID recordedById;
    private String recordedByName;
    private LocalDateTime transactionDate;
    private LocalDateTime createdAt;
}
