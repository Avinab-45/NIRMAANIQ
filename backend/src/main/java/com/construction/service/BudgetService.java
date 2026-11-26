package com.construction.service;

import com.construction.dto.budget.*;
import com.construction.entity.*;
import com.construction.entity.enums.AuditAction;
import com.construction.entity.enums.CostCategory;
import com.construction.entity.enums.PaymentStatus;
import com.construction.exception.ResourceNotFoundException;
import com.construction.repository.*;
import com.construction.security.ProjectAccessService;
import com.construction.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetLineRepository budgetLineRepository;
    private final CostEntryRepository costEntryRepository;
    private final ProjectPhaseRepository projectPhaseRepository;
    private final ProfileRepository profileRepository;
    private final ProjectAccessService projectAccessService;
    private final AuditLogService auditLogService;

    @Transactional
    public BudgetLineResponse createBudgetLine(String projectCode, CreateBudgetLineRequest request) {
        Project project = projectAccessService.validateProjectOwner(projectCode);
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        ProjectPhase phase = null;
        if (request.getPhaseId() != null) {
            phase = projectPhaseRepository.findByIdAndProject_ProjectCode(request.getPhaseId(), projectCode)
                    .orElse(null);
        }

        BudgetLine line = BudgetLine.builder()
                .project(project)
                .phase(phase)
                .category(request.getCategory())
                .plannedAmount(request.getPlannedAmount())
                .description(request.getDescription())
                .build();

        line = budgetLineRepository.save(line);

        auditLogService.record("budget_lines", line.getId().toString(), AuditAction.CREATE, currentUserId,
                "Created budget line for " + line.getCategory() + ": $" + line.getPlannedAmount() + " in project " + projectCode);

        return mapToBudgetLineResponse(line);
    }

    @Transactional(readOnly = true)
    public List<BudgetLineResponse> getBudgetLines(String projectCode) {
        projectAccessService.validateProjectReadAccess(projectCode);
        return budgetLineRepository.findByProject_ProjectCodeOrderByCreatedAtAsc(projectCode)
                .stream()
                .map(this::mapToBudgetLineResponse)
                .toList();
    }

    @Transactional
    public CostEntryResponse createExpense(String projectCode, CreateExpenseRequest request) {
        Project project = projectAccessService.validateProjectWriteAccess(projectCode);
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Profile recordedBy = profileRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found: " + currentUserId));

        BudgetLine budgetLine = null;
        if (request.getBudgetLineId() != null) {
            budgetLine = budgetLineRepository.findByIdAndProject_ProjectCode(request.getBudgetLineId(), projectCode)
                    .orElse(null);
        }

        CostEntry costEntry = CostEntry.builder()
                .project(project)
                .budgetLine(budgetLine)
                .amount(request.getAmount())
                .expenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : LocalDate.now())
                .category(request.getCategory().trim().toUpperCase())
                .vendor(request.getVendor())
                .invoiceNumber(request.getInvoiceNumber())
                .description(request.getDescription())
                .paymentStatus(PaymentStatus.APPROVED)
                .recordedBy(recordedBy)
                .build();

        costEntry = costEntryRepository.save(costEntry);

        auditLogService.record("cost_entries", costEntry.getId().toString(), AuditAction.CREATE, currentUserId,
                "Recorded expense: $" + costEntry.getAmount() + " (" + costEntry.getCategory() + ") in project " + projectCode);

        return mapToCostEntryResponse(costEntry);
    }

    @Transactional(readOnly = true)
    public Page<CostEntryResponse> getExpenses(String projectCode, Pageable pageable) {
        projectAccessService.validateProjectReadAccess(projectCode);
        return costEntryRepository.findByProject_ProjectCodeOrderByExpenseDateDesc(projectCode, pageable)
                .map(this::mapToCostEntryResponse);
    }

    @Transactional(readOnly = true)
    public FinancialSummaryResponse getFinancialSummary(String projectCode) {
        projectAccessService.validateProjectReadAccess(projectCode);

        List<BudgetLine> budgetLines = budgetLineRepository.findByProject_ProjectCodeOrderByCreatedAtAsc(projectCode);
        List<CostEntry> costEntries = costEntryRepository.findByProject_ProjectCode(projectCode);

        BigDecimal totalPlanned = budgetLines.stream()
                .map(BudgetLine::getPlannedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalActual = costEntries.stream()
                .map(CostEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remaining = totalPlanned.subtract(totalActual);
        BigDecimal variance = totalPlanned.subtract(totalActual);

        BigDecimal percentageSpent = BigDecimal.ZERO;
        if (totalPlanned.compareTo(BigDecimal.ZERO) > 0) {
            percentageSpent = totalActual.divide(totalPlanned, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        Map<String, BigDecimal> expensesByCategory = new HashMap<>();
        for (CostEntry entry : costEntries) {
            expensesByCategory.merge(entry.getCategory(), entry.getAmount(), BigDecimal::add);
        }

        Map<String, BigDecimal> budgetByCategory = new HashMap<>();
        for (BudgetLine bl : budgetLines) {
            budgetByCategory.merge(bl.getCategory().name(), bl.getPlannedAmount(), BigDecimal::add);
        }

        return FinancialSummaryResponse.builder()
                .projectCode(projectCode)
                .totalPlannedBudget(totalPlanned)
                .totalActualExpenses(totalActual)
                .remainingBudget(remaining)
                .budgetVariance(variance)
                .percentageSpent(percentageSpent)
                .expensesByCategory(expensesByCategory)
                .budgetByCategory(budgetByCategory)
                .build();
    }

    private BudgetLineResponse mapToBudgetLineResponse(BudgetLine bl) {
        return BudgetLineResponse.builder()
                .id(bl.getId())
                .phaseId(bl.getPhase() != null ? bl.getPhase().getId() : null)
                .phaseName(bl.getPhase() != null ? bl.getPhase().getName() : null)
                .category(bl.getCategory())
                .plannedAmount(bl.getPlannedAmount())
                .description(bl.getDescription())
                .createdAt(bl.getCreatedAt())
                .updatedAt(bl.getUpdatedAt())
                .build();
    }

    private CostEntryResponse mapToCostEntryResponse(CostEntry ce) {
        return CostEntryResponse.builder()
                .id(ce.getId())
                .budgetLineId(ce.getBudgetLine() != null ? ce.getBudgetLine().getId() : null)
                .amount(ce.getAmount())
                .expenseDate(ce.getExpenseDate())
                .category(ce.getCategory())
                .vendor(ce.getVendor())
                .invoiceNumber(ce.getInvoiceNumber())
                .description(ce.getDescription())
                .paymentStatus(ce.getPaymentStatus())
                .recordedById(ce.getRecordedBy().getId())
                .recordedByName(ce.getRecordedBy().getFullName() != null ? ce.getRecordedBy().getFullName() : ce.getRecordedBy().getEmail())
                .createdAt(ce.getCreatedAt())
                .build();
    }
}
