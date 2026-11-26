package com.construction.service;

import com.construction.dto.material.CreateMaterialItemRequest;
import com.construction.dto.material.MaterialItemResponse;
import com.construction.dto.material.MaterialTransactionResponse;
import com.construction.dto.material.RecordTransactionRequest;
import com.construction.entity.InventoryLevel;
import com.construction.entity.MaterialItem;
import com.construction.entity.MaterialTransaction;
import com.construction.entity.Profile;
import com.construction.entity.Project;
import com.construction.entity.enums.AccountStatus;
import com.construction.entity.enums.NotificationType;
import com.construction.entity.enums.TransactionType;
import com.construction.repository.InventoryLevelRepository;
import com.construction.repository.MaterialItemRepository;
import com.construction.repository.MaterialTransactionRepository;
import com.construction.repository.ProfileRepository;
import com.construction.security.ProjectAccessService;
import com.construction.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private MaterialItemRepository materialItemRepository;

    @Mock
    private MaterialTransactionRepository materialTransactionRepository;

    @Mock
    private InventoryLevelRepository inventoryLevelRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID userId;
    private Project project;
    private Profile profile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        Profile owner = Profile.builder().id(UUID.randomUUID()).email("owner@test.com").build();
        profile = Profile.builder().id(userId).email("contractor@test.com").status(AccountStatus.ACTIVE).build();

        project = Project.builder()
                .id(UUID.randomUUID())
                .projectCode("PRJ-INV1")
                .name("Inventory Project")
                .createdBy(owner)
                .build();

        UserPrincipal principal = UserPrincipal.builder()
                .id(userId)
                .email("contractor@test.com")
                .status(AccountStatus.ACTIVE)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CONTRACTOR")))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Create material item initializes inventory level to zero")
    void createMaterialItem_InitializesInventoryToZero() {
        CreateMaterialItemRequest request = CreateMaterialItemRequest.builder()
                .itemCode("CEM-53")
                .name("OPC 53 Grade Cement")
                .unitOfMeasure("BAGS")
                .minimumThreshold(new BigDecimal("50.00"))
                .category("Cement")
                .build();

        when(projectAccessService.validateProjectWriteAccess("PRJ-INV1")).thenReturn(project);
        when(materialItemRepository.existsByProject_IdAndItemCode(project.getId(), "CEM-53")).thenReturn(false);
        when(materialItemRepository.save(any(MaterialItem.class))).thenAnswer(i -> {
            MaterialItem item = i.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        MaterialItemResponse response = inventoryService.createMaterialItem("PRJ-INV1", request);

        assertNotNull(response);
        assertEquals("CEM-53", response.getItemCode());
        assertEquals(BigDecimal.ZERO, response.getCurrentStock());

        verify(inventoryLevelRepository).save(argThat(inv ->
                inv.getCurrentStock().compareTo(BigDecimal.ZERO) == 0 &&
                inv.getMaterialItem().getItemCode().equals("CEM-53")
        ));
    }

    @Test
    @DisplayName("Material transaction RECEIVED increases inventory stock")
    void recordTransaction_Received_IncreasesStock() {
        MaterialItem item = MaterialItem.builder()
                .id(UUID.randomUUID())
                .project(project)
                .itemCode("STEEL-12")
                .name("12mm TMT Rebars")
                .unitOfMeasure("TONNES")
                .minimumThreshold(new BigDecimal("5.00"))
                .build();

        InventoryLevel currentInventory = InventoryLevel.builder()
                .id(UUID.randomUUID())
                .project(project)
                .materialItem(item)
                .currentStock(new BigDecimal("10.00"))
                .build();

        when(projectAccessService.validateProjectWriteAccess("PRJ-INV1")).thenReturn(project);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(materialItemRepository.findByProject_ProjectCodeAndItemCode("PRJ-INV1", "STEEL-12"))
                .thenReturn(Optional.of(item));
        when(inventoryLevelRepository.findByProject_IdAndMaterialItem_Id(project.getId(), item.getId()))
                .thenReturn(Optional.of(currentInventory));
        when(materialTransactionRepository.save(any(MaterialTransaction.class))).thenAnswer(i -> {
            MaterialTransaction t = i.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        RecordTransactionRequest request = RecordTransactionRequest.builder()
                .itemCode("STEEL-12")
                .transactionType(TransactionType.RECEIVED)
                .quantity(new BigDecimal("15.00"))
                .unitPrice(new BigDecimal("800.00"))
                .build();

        MaterialTransactionResponse response = inventoryService.recordTransaction("PRJ-INV1", request);

        assertNotNull(response);
        assertEquals(new BigDecimal("25.00"), currentInventory.getCurrentStock());
        verify(inventoryLevelRepository).save(currentInventory);
    }

    @Test
    @DisplayName("Material transaction USED triggers low stock alert when below threshold")
    void recordTransaction_Used_TriggersLowStockAlert() {
        MaterialItem item = MaterialItem.builder()
                .id(UUID.randomUUID())
                .project(project)
                .itemCode("BRICK-RED")
                .name("Red Clay Bricks")
                .unitOfMeasure("UNITS")
                .minimumThreshold(new BigDecimal("500.00"))
                .build();

        InventoryLevel currentInventory = InventoryLevel.builder()
                .id(UUID.randomUUID())
                .project(project)
                .materialItem(item)
                .currentStock(new BigDecimal("600.00"))
                .build();

        when(projectAccessService.validateProjectWriteAccess("PRJ-INV1")).thenReturn(project);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(materialItemRepository.findByProject_ProjectCodeAndItemCode("PRJ-INV1", "BRICK-RED"))
                .thenReturn(Optional.of(item));
        when(inventoryLevelRepository.findByProject_IdAndMaterialItem_Id(project.getId(), item.getId()))
                .thenReturn(Optional.of(currentInventory));
        when(materialTransactionRepository.save(any(MaterialTransaction.class))).thenAnswer(i -> {
            MaterialTransaction t = i.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        RecordTransactionRequest request = RecordTransactionRequest.builder()
                .itemCode("BRICK-RED")
                .transactionType(TransactionType.USED)
                .quantity(new BigDecimal("200.00")) // 600 - 200 = 400 <= threshold 500
                .build();

        inventoryService.recordTransaction("PRJ-INV1", request);

        assertEquals(new BigDecimal("400.00"), currentInventory.getCurrentStock());
        verify(notificationService).createNotification(
                eq(project.getCreatedBy().getId()),
                eq(project),
                contains("Low Inventory Alert"),
                anyString(),
                eq(NotificationType.INVENTORY_WARNING)
        );
    }
}
