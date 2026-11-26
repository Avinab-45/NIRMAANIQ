package com.construction.controller;

import com.construction.dto.budget.*;
import com.construction.dto.common.ApiResponse;
import com.construction.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectCode}")
@RequiredArgsConstructor
public class BudgetExpenseController {

    private final BudgetService budgetService;

    @PostMapping("/budgets")
    public ResponseEntity<ApiResponse<BudgetLineResponse>> createBudgetLine(
            @PathVariable String projectCode,
            @Valid @RequestBody CreateBudgetLineRequest request) {
        BudgetLineResponse response = budgetService.createBudgetLine(projectCode, request);
        return new ResponseEntity<>(ApiResponse.success("Budget line created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/budgets")
    public ResponseEntity<ApiResponse<List<BudgetLineResponse>>> getBudgetLines(
            @PathVariable String projectCode) {
        List<BudgetLineResponse> response = budgetService.getBudgetLines(projectCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/expenses")
    public ResponseEntity<ApiResponse<CostEntryResponse>> createExpense(
            @PathVariable String projectCode,
            @Valid @RequestBody CreateExpenseRequest request) {
        CostEntryResponse response = budgetService.createExpense(projectCode, request);
        return new ResponseEntity<>(ApiResponse.success("Expense recorded successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/expenses")
    public ResponseEntity<ApiResponse<Page<CostEntryResponse>>> getExpenses(
            @PathVariable String projectCode,
            @PageableDefault(size = 20, sort = "expenseDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CostEntryResponse> response = budgetService.getExpenses(projectCode, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/financial-summary")
    public ResponseEntity<ApiResponse<FinancialSummaryResponse>> getFinancialSummary(
            @PathVariable String projectCode) {
        FinancialSummaryResponse response = budgetService.getFinancialSummary(projectCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
