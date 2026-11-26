package com.construction.service;

import com.construction.dto.material.*;
import com.construction.entity.*;
import com.construction.entity.enums.AuditAction;
import com.construction.entity.enums.NotificationType;
import com.construction.entity.enums.TransactionType;
import com.construction.exception.BadRequestException;
import com.construction.exception.ResourceNotFoundException;
import com.construction.repository.*;
import com.construction.security.ProjectAccessService;
import com.construction.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final MaterialItemRepository materialItemRepository;
    private final MaterialTransactionRepository materialTransactionRepository;
    private final InventoryLevelRepository inventoryLevelRepository;
    private final ProfileRepository profileRepository;
    private final ProjectAccessService projectAccessService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    /**
     * Creates a material item catalog entry for the project.
     * Initializes inventory level to 0.
     */
    @Transactional
    public MaterialItemResponse createMaterialItem(String projectCode, CreateMaterialItemRequest request) {
        Project project = projectAccessService.validateProjectWriteAccess(projectCode);
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        String itemCode = request.getItemCode().trim().toUpperCase();
        if (materialItemRepository.existsByProject_IdAndItemCode(project.getId(), itemCode)) {
            throw new BadRequestException("Material item with code '" + itemCode + "' already exists in this project.");
        }

        MaterialItem item = MaterialItem.builder()
                .project(project)
                .itemCode(itemCode)
                .name(request.getName().trim())
                .category(request.getCategory())
                .unitOfMeasure(request.getUnitOfMeasure().trim().toUpperCase())
                .minimumThreshold(request.getMinimumThreshold() != null ? request.getMinimumThreshold() : BigDecimal.ZERO)
                .build();

        item = materialItemRepository.save(item);

        // Initialize inventory level record
        InventoryLevel inventoryLevel = InventoryLevel.builder()
                .project(project)
                .materialItem(item)
                .currentStock(BigDecimal.ZERO)
                .reservedStock(BigDecimal.ZERO)
                .lastUpdated(LocalDateTime.now())
                .build();
        inventoryLevelRepository.save(inventoryLevel);

        auditLogService.record("material_items", item.getId().toString(), AuditAction.CREATE, currentUserId,
                "Created material item: " + item.getName() + " (" + item.getItemCode() + ") for project " + projectCode);

        return mapToItemResponse(item, BigDecimal.ZERO);
    }

    /**
     * Lists all material items with current stock.
     */
    @Transactional(readOnly = true)
    public List<MaterialItemResponse> getMaterialItems(String projectCode) {
        projectAccessService.validateProjectReadAccess(projectCode);
        List<MaterialItem> items = materialItemRepository.findByProject_ProjectCodeOrderByNameAsc(projectCode);

        return items.stream().map(item -> {
            BigDecimal currentStock = inventoryLevelRepository
                    .findByProject_IdAndMaterialItem_Id(item.getProject().getId(), item.getId())
                    .map(InventoryLevel::getCurrentStock)
                    .orElse(BigDecimal.ZERO);
            return mapToItemResponse(item, currentStock);
        }).toList();
    }

    /**
     * Records a material transaction (RECEIVED, USED, ADJUSTMENT, RETURN).
     * Atomically adjusts inventory levels and triggers low-stock alerts.
     */
    @Transactional
    public MaterialTransactionResponse recordTransaction(String projectCode, RecordTransactionRequest request) {
        Project project = projectAccessService.validateProjectWriteAccess(projectCode);
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Profile recordedBy = profileRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found: " + currentUserId));

        String itemCode = request.getItemCode().trim().toUpperCase();
        MaterialItem item = materialItemRepository.findByProject_ProjectCodeAndItemCode(projectCode, itemCode)
                .orElseThrow(() -> new ResourceNotFoundException("Material item not found with code: " + itemCode));

        InventoryLevel inventory = inventoryLevelRepository
                .findByProject_IdAndMaterialItem_Id(project.getId(), item.getId())
                .orElseGet(() -> InventoryLevel.builder()
                        .project(project)
                        .materialItem(item)
                        .currentStock(BigDecimal.ZERO)
                        .reservedStock(BigDecimal.ZERO)
                        .lastUpdated(LocalDateTime.now())
                        .build());

        BigDecimal qty = request.getQuantity();
        BigDecimal newStock = inventory.getCurrentStock();

        switch (request.getTransactionType()) {
            case RECEIVED, RETURN -> newStock = newStock.add(qty);
            case USED -> {
                if (newStock.compareTo(qty) < 0) {
                    log.warn("Recording usage of {} exceeding current stock {} for item {}", qty, newStock, itemCode);
                }
                newStock = newStock.subtract(qty);
            }
            case ADJUSTMENT -> newStock = qty;
        }

        inventory.setCurrentStock(newStock);
        inventory.setLastUpdated(LocalDateTime.now());
        inventoryLevelRepository.save(inventory);

        // Calculate total cost
        BigDecimal unitPrice = request.getUnitPrice();
        BigDecimal totalCost = null;
        if (unitPrice != null) {
            totalCost = unitPrice.multiply(qty);
        }

        MaterialTransaction transaction = MaterialTransaction.builder()
                .project(project)
                .materialItem(item)
                .transactionType(request.getTransactionType())
                .quantity(qty)
                .unitPrice(unitPrice)
                .totalCost(totalCost)
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .recordedBy(recordedBy)
                .transactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDateTime.now())
                .build();

        transaction = materialTransactionRepository.save(transaction);

        // Check for low stock warning
        if (item.getMinimumThreshold() != null && newStock.compareTo(item.getMinimumThreshold()) <= 0) {
            notificationService.createNotification(
                    project.getCreatedBy().getId(),
                    project,
                    "Low Inventory Alert: " + item.getName(),
                    "Stock for " + item.getName() + " (" + item.getItemCode() + ") has reached " + newStock + " " + item.getUnitOfMeasure() + ", which is at or below threshold (" + item.getMinimumThreshold() + ").",
                    NotificationType.INVENTORY_WARNING
            );
        }

        auditLogService.record("material_transactions", transaction.getId().toString(), AuditAction.CREATE, currentUserId,
                "Recorded " + transaction.getTransactionType() + " of " + qty + " " + item.getUnitOfMeasure() + " for " + item.getItemCode() + ". New stock: " + newStock);

        return mapToTransactionResponse(transaction);
    }

    /**
     * Returns full inventory levels for the project.
     */
    @Transactional(readOnly = true)
    public List<InventoryLevelResponse> getInventory(String projectCode) {
        projectAccessService.validateProjectReadAccess(projectCode);
        return inventoryLevelRepository.findByProject_ProjectCode(projectCode)
                .stream()
                .map(this::mapToInventoryResponse)
                .toList();
    }

    private MaterialItemResponse mapToItemResponse(MaterialItem item, BigDecimal currentStock) {
        boolean isLow = item.getMinimumThreshold() != null && currentStock.compareTo(item.getMinimumThreshold()) <= 0;
        return MaterialItemResponse.builder()
                .id(item.getId())
                .itemCode(item.getItemCode())
                .name(item.getName())
                .category(item.getCategory())
                .unitOfMeasure(item.getUnitOfMeasure())
                .minimumThreshold(item.getMinimumThreshold())
                .currentStock(currentStock)
                .isLowStock(isLow)
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private MaterialTransactionResponse mapToTransactionResponse(MaterialTransaction t) {
        return MaterialTransactionResponse.builder()
                .id(t.getId())
                .itemCode(t.getMaterialItem().getItemCode())
                .itemName(t.getMaterialItem().getName())
                .transactionType(t.getTransactionType())
                .quantity(t.getQuantity())
                .unitPrice(t.getUnitPrice())
                .totalCost(t.getTotalCost())
                .referenceNumber(t.getReferenceNumber())
                .notes(t.getNotes())
                .recordedById(t.getRecordedBy().getId())
                .recordedByName(t.getRecordedBy().getFullName() != null ? t.getRecordedBy().getFullName() : t.getRecordedBy().getEmail())
                .transactionDate(t.getTransactionDate())
                .createdAt(t.getCreatedAt())
                .build();
    }

    private InventoryLevelResponse mapToInventoryResponse(InventoryLevel inv) {
        BigDecimal threshold = inv.getMaterialItem().getMinimumThreshold();
        boolean isLow = threshold != null && inv.getCurrentStock().compareTo(threshold) <= 0;
        return InventoryLevelResponse.builder()
                .id(inv.getId())
                .materialItemId(inv.getMaterialItem().getId())
                .itemCode(inv.getMaterialItem().getItemCode())
                .itemName(inv.getMaterialItem().getName())
                .category(inv.getMaterialItem().getCategory())
                .unitOfMeasure(inv.getMaterialItem().getUnitOfMeasure())
                .currentStock(inv.getCurrentStock())
                .reservedStock(inv.getReservedStock())
                .minimumThreshold(threshold)
                .isLowStock(isLow)
                .lastUpdated(inv.getLastUpdated())
                .build();
    }
}
