package com.construction.controller;

import com.construction.dto.common.ApiResponse;
import com.construction.dto.phase.CreatePhaseRequest;
import com.construction.dto.phase.PhaseResponse;
import com.construction.dto.phase.UpdatePhaseRequest;
import com.construction.service.ProjectPhaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectCode}/phases")
@RequiredArgsConstructor
public class ProjectPhaseController {

    private final ProjectPhaseService projectPhaseService;

    @PostMapping
    public ResponseEntity<ApiResponse<PhaseResponse>> createPhase(
            @PathVariable String projectCode,
            @Valid @RequestBody CreatePhaseRequest request) {
        PhaseResponse response = projectPhaseService.createPhase(projectCode, request);
        return new ResponseEntity<>(ApiResponse.success("Project phase created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PhaseResponse>>> getPhases(
            @PathVariable String projectCode) {
        List<PhaseResponse> response = projectPhaseService.getPhases(projectCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{phaseId}")
    public ResponseEntity<ApiResponse<PhaseResponse>> updatePhase(
            @PathVariable String projectCode,
            @PathVariable UUID phaseId,
            @Valid @RequestBody UpdatePhaseRequest request) {
        PhaseResponse response = projectPhaseService.updatePhase(projectCode, phaseId, request);
        return ResponseEntity.ok(ApiResponse.success("Project phase updated successfully", response));
    }
}
