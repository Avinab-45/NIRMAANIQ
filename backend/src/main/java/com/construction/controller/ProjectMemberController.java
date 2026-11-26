package com.construction.controller;

import com.construction.dto.common.ApiResponse;
import com.construction.dto.member.AddMemberRequest;
import com.construction.dto.member.ProjectMemberResponse;
import com.construction.service.ProjectMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectCode}/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> addMember(
            @PathVariable String projectCode,
            @Valid @RequestBody AddMemberRequest request) {
        ProjectMemberResponse response = projectMemberService.addMember(projectCode, request);
        return new ResponseEntity<>(ApiResponse.success("Project member assigned successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectMemberResponse>>> getMembers(
            @PathVariable String projectCode) {
        List<ProjectMemberResponse> response = projectMemberService.getMembers(projectCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable String projectCode,
            @PathVariable UUID userId) {
        projectMemberService.removeMember(projectCode, userId);
        return ResponseEntity.ok(ApiResponse.success("Member removed from project successfully", null));
    }
}
