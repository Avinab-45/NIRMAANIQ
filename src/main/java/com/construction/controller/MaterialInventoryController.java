package com.construction.controller;

import com.construction.dto.common.ApiResponse;
import com.construction.dto.material.*;
import com.construction.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectCode}")
@RequiredArgsConstructor
public class MaterialInventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/materials")
    public ResponseEntity<ApiResponse<MaterialItemResponse>> createMaterialItem(
            @PathVariable String projectCode,
            @Valid @RequestBody CreateMaterialItemRequest request) {
        MaterialItemResponse response = inventoryService.createMaterialItem(projectCode, request);
        return new ResponseEntity<>(ApiResponse.success("Material item created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/materials")
    public ResponseEntity<ApiResponse<List<MaterialItemResponse>>> getMaterialItems(
            @PathVariable String projectCode) {
        List<MaterialItemResponse> response = inventoryService.getMaterialItems(projectCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/materials/transactions")
    public ResponseEntity<ApiResponse<MaterialTransactionResponse>> recordTransaction(
            @PathVariable String projectCode,
            @Valid @RequestBody RecordTransactionRequest request) {
        MaterialTransactionResponse response = inventoryService.recordTransaction(projectCode, request);
        return new ResponseEntity<>(ApiResponse.success("Material transaction recorded successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<List<InventoryLevelResponse>>> getInventory(
            @PathVariable String projectCode) {
        List<InventoryLevelResponse> response = inventoryService.getInventory(projectCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
