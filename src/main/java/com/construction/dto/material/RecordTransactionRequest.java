package com.construction.dto.material;

import com.construction.entity.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordTransactionRequest {

    @NotBlank(message = "Item code is required")
    private String itemCode;

    @NotNull(message = "Transaction type is required (RECEIVED, USED, ADJUSTMENT, RETURN)")
    private TransactionType transactionType;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    private BigDecimal unitPrice;
    private String referenceNumber;
    private String notes;
    private LocalDateTime transactionDate;
}
