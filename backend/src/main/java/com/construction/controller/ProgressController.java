package com.construction.controller;

import com.construction.dto.common.ApiResponse;
import com.construction.dto.progress.CreateProgressRequest;
import com.construction.dto.progress.ProgressResponse;
import com.construction.service.ProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectCode}/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProgressResponse>> createProgressUpdate(
            @PathVariable String projectCode,
            @Valid @RequestBody CreateProgressRequest request) {
        ProgressResponse response = progressService.createProgressUpdate(projectCode, request);
        return new ResponseEntity<>(ApiResponse.success("Progress update recorded successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getProgressUpdates(
            @PathVariable String projectCode,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        if (date != null) {
            List<ProgressResponse> dateProgress = progressService.getProgressUpdatesByDate(projectCode, date);
            return ResponseEntity.ok(ApiResponse.success(dateProgress));
        }

        if (startDate != null && endDate != null) {
            List<ProgressResponse> rangeProgress = progressService.getProgressUpdatesByDateRange(projectCode, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success(rangeProgress));
        }

        Page<ProgressResponse> response = progressService.getProgressUpdates(projectCode, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<ProgressResponse>>> getProgressUpdatesBySpecificDate(
            @PathVariable String projectCode,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ProgressResponse> response = progressService.getProgressUpdatesByDate(projectCode, date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<ProgressResponse>> getLatestProgressUpdate(
            @PathVariable String projectCode) {
        ProgressResponse response = progressService.getLatestProgressUpdate(projectCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
