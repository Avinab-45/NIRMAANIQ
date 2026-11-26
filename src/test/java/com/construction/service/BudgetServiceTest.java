package com.construction.service;

import com.construction.dto.budget.*;
import com.construction.entity.*;
import com.construction.entity.enums.AccountStatus;
import com.construction.entity.enums.CostCategory;
import com.construction.repository.*;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetLineRepository budgetLineRepository;

    @Mock
    private CostEntryRepository costEntryRepository;

    @Mock
    private ProjectPhaseRepository projectPhaseRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private BudgetService budgetService;

    private UUID userId;
    private Project project;
    private Profile profile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        profile = Profile.builder().id(userId).email("builder@test.com").status(AccountStatus.ACTIVE).build();

        project = Project.builder()
                .id(UUID.randomUUID())
                .projectCode("PRJ-BUDGET1")
                .name("Budget Project")
                .createdBy(profile)
                .build();

        UserPrincipal principal = UserPrincipal.builder()
                .id(userId)
                .email("builder@test.com")
                .status(AccountStatus.ACTIVE)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_BUILDER")))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Create budget line persists category and planned amount")
    void createBudgetLine_Success() {
        CreateBudgetLineRequest request = CreateBudgetLineRequest.builder()
                .category(CostCategory.MATERIALS)
                .plannedAmount(new BigDecimal("100000.00"))
                .description("Concrete and steel budget")
                .build();

        when(projectAccessService.validateProjectOwner("PRJ-BUDGET1")).thenReturn(project);
        when(budgetLineRepository.save(any(BudgetLine.class))).thenAnswer(i -> {
            BudgetLine bl = i.getArgument(0);
            bl.setId(UUID.randomUUID());
            return bl;
        });

        BudgetLineResponse response = budgetService.createBudgetLine("PRJ-BUDGET1", request);

        assertNotNull(response);
        assertEquals(CostCategory.MATERIALS, response.getCategory());
        assertEquals(new BigDecimal("100000.00"), response.getPlannedAmount());
        verify(auditLogService).record(eq("budget_lines"), anyString(), any(), eq(userId), anyString());
    }

    @Test
    @DisplayName("Create expense entry records actual cost")
    void createExpense_Success() {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .amount(new BigDecimal("5400.00"))
                .expenseDate(LocalDate.now())
                .category("EQUIPMENT")
                .vendor("Heavy Machinery Co")
                .invoiceNumber("INV-8899")
                .description("Excavator rental for foundation")
                .build();

        when(projectAccessService.validateProjectWriteAccess("PRJ-BUDGET1")).thenReturn(project);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(costEntryRepository.save(any(CostEntry.class))).thenAnswer(i -> {
            CostEntry ce = i.getArgument(0);
            ce.setId(UUID.randomUUID());
            return ce;
        });

        CostEntryResponse response = budgetService.createExpense("PRJ-BUDGET1", request);

        assertNotNull(response);
        assertEquals(new BigDecimal("5400.00"), response.getAmount());
        assertEquals("EQUIPMENT", response.getCategory());
        assertEquals("Heavy Machinery Co", response.getVendor());
    }

    @Test
    @DisplayName("Financial summary correctly calculates planned, actual, variance, and percentage spent")
    void getFinancialSummary_CalculatesCorrectly() {
        BudgetLine bl1 = BudgetLine.builder().plannedAmount(new BigDecimal("60000.00")).category(CostCategory.MATERIALS).build();
        BudgetLine bl2 = BudgetLine.builder().plannedAmount(new BigDecimal("40000.00")).category(CostCategory.LABOUR).build();

        CostEntry ce1 = CostEntry.builder().amount(new BigDecimal("30000.00")).category("MATERIALS").build();
        CostEntry ce2 = CostEntry.builder().amount(new BigDecimal("10000.00")).category("LABOUR").build();

        when(projectAccessService.validateProjectReadAccess("PRJ-BUDGET1")).thenReturn(project);
        when(budgetLineRepository.findByProject_ProjectCodeOrderByCreatedAtAsc("PRJ-BUDGET1"))
                .thenReturn(List.of(bl1, bl2));
        when(costEntryRepository.findByProject_ProjectCode("PRJ-BUDGET1"))
                .thenReturn(List.of(ce1, ce2));

        FinancialSummaryResponse summary = budgetService.getFinancialSummary("PRJ-BUDGET1");

        assertNotNull(summary);
        assertEquals(new BigDecimal("100000.00"), summary.getTotalPlannedBudget());
        assertEquals(new BigDecimal("40000.00"), summary.getTotalActualExpenses());
        assertEquals(new BigDecimal("60000.00"), summary.getRemainingBudget());
        assertEquals(new BigDecimal("40.0000"), summary.getPercentageSpent());
    }
}
