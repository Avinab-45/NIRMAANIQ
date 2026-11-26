package com.construction.dto.budget;

import com.construction.entity.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostEntryResponse {
    private UUID id;
    private UUID budgetLineId;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String category;
    private String vendor;
    private String invoiceNumber;
    private String description;
    private PaymentStatus paymentStatus;
    private UUID recordedById;
    private String recordedByName;
    private LocalDateTime createdAt;
}
