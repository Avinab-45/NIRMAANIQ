package com.construction.controller;

import com.construction.dto.common.ApiResponse;
import com.construction.dto.project.CreateProjectRequest;
import com.construction.dto.project.ProjectResponse;
import com.construction.dto.project.UpdateProjectRequest;
import com.construction.service.ProjectService;
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
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return new ResponseEntity<>(ApiResponse.success("Project created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProjectResponse>>> getProjects(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProjectResponse> response = projectService.getProjects(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{projectCode}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectByCode(
            @PathVariable String projectCode) {
        ProjectResponse response = projectService.getProjectByCode(projectCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{projectCode}")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable String projectCode,
            @Valid @RequestBody UpdateProjectRequest request) {
        ProjectResponse response = projectService.updateProject(projectCode, request);
        return ResponseEntity.ok(ApiResponse.success("Project updated successfully", response));
    }

    @DeleteMapping("/{projectCode}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable String projectCode) {
        projectService.deleteProject(projectCode);
        return ResponseEntity.ok(ApiResponse.success("Project archived successfully", null));
    }
}
