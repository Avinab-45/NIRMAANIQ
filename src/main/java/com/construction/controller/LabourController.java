package com.construction.controller;

import com.construction.dto.common.ApiResponse;
import com.construction.dto.labour.CreateLabourRecordRequest;
import com.construction.dto.labour.LabourRecordResponse;
import com.construction.service.LabourService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectCode}/labour")
@RequiredArgsConstructor
public class LabourController {

    private final LabourService labourService;

    @PostMapping
    public ResponseEntity<ApiResponse<LabourRecordResponse>> createLabourRecord(
            @PathVariable String projectCode,
            @Valid @RequestBody CreateLabourRecordRequest request) {
        LabourRecordResponse response = labourService.createLabourRecord(projectCode, request);
        return new ResponseEntity<>(ApiResponse.success("Labour record created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<LabourRecordResponse>>> getLabourRecords(
            @PathVariable String projectCode,
            @PageableDefault(size = 20, sort = "workDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<LabourRecordResponse> response = labourService.getLabourRecords(projectCode, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
