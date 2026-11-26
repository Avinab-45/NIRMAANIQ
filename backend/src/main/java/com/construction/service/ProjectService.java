package com.construction.service;

import com.construction.dto.project.CreateProjectRequest;
import com.construction.dto.project.ProjectResponse;
import com.construction.dto.project.UpdateProjectRequest;
import com.construction.entity.Profile;
import com.construction.entity.Project;
import com.construction.entity.ProjectMember;
import com.construction.entity.enums.AuditAction;
import com.construction.entity.enums.MemberStatus;
import com.construction.entity.enums.ProjectMemberRole;
import com.construction.entity.enums.ProjectStatus;
import com.construction.entity.enums.RoleType;
import com.construction.exception.ResourceNotFoundException;
import com.construction.repository.ProfileRepository;
import com.construction.repository.ProjectMemberRepository;
import com.construction.repository.ProjectRepository;
import com.construction.security.ProjectAccessService;
import com.construction.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProfileRepository profileRepository;
    private final ProjectAccessService projectAccessService;
    private final AuditLogService auditLogService;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Builder creates a new project.
     * Automatically generates a unique project code (e.g. PRJ-9X8K2M)
     * and sets the builder as OWNER in project_members.
     */
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        SecurityUtil.requireRole(RoleType.ROLE_BUILDER);
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Profile builderProfile = profileRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Builder profile not found: " + currentUserId));

        String projectCode = generateUniqueProjectCode();

        Project project = Project.builder()
                .projectCode(projectCode)
                .name(request.getName().trim())
                .description(request.getDescription())
                .location(request.getLocation())
                .status(ProjectStatus.PLANNING)
                .plannedStartDate(request.getPlannedStartDate())
                .plannedEndDate(request.getPlannedEndDate())
                .createdBy(builderProfile)
                .build();

        project = projectRepository.save(project);

        // Assign Builder as OWNER in project_members
        ProjectMember ownerMember = ProjectMember.builder()
                .project(project)
                .user(builderProfile)
                .role(ProjectMemberRole.OWNER)
                .status(MemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();
        projectMemberRepository.save(ownerMember);

        auditLogService.record("projects", project.getProjectCode(), AuditAction.CREATE, currentUserId,
                "Project created: " + project.getName() + " with code " + projectCode);

        log.info("Project created successfully: {} by builder {}", projectCode, currentUserId);
        return mapToResponse(project);
    }

    /**
     * Lists projects accessible to the current authenticated user.
     * - Builder: projects they own.
     * - Contractor / Client: projects assigned to them.
     */
    @Transactional(readOnly = true)
    public Page<ProjectResponse> getProjects(Pageable pageable) {
        SecurityUtil.requireActiveAccount();
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        if (SecurityUtil.hasRole(RoleType.ROLE_BUILDER)) {
            return projectRepository.findByCreatedBy_Id(currentUserId, pageable)
                    .map(this::mapToResponse);
        } else {
            return projectRepository.findAssignedProjectsByUserId(currentUserId, pageable)
                    .map(this::mapToResponse);
        }
    }

    /**
     * Retrieves details for a specific project by project code.
     * Validates that the current user has read permissions.
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProjectByCode(String projectCode) {
        Project project = projectAccessService.validateProjectReadAccess(projectCode);
        return mapToResponse(project);
    }

    /**
     * Updates project details. Builder owner only.
     */
    @Transactional
    public ProjectResponse updateProject(String projectCode, UpdateProjectRequest request) {
        Project project = projectAccessService.validateProjectOwner(projectCode);
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        if (request.getName() != null && !request.getName().isBlank()) {
            project.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            project.setLocation(request.getLocation());
        }
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }
        if (request.getPlannedStartDate() != null) {
            project.setPlannedStartDate(request.getPlannedStartDate());
        }
        if (request.getPlannedEndDate() != null) {
            project.setPlannedEndDate(request.getPlannedEndDate());
        }
        if (request.getActualStartDate() != null) {
            project.setActualStartDate(request.getActualStartDate());
        }
        if (request.getActualEndDate() != null) {
            project.setActualEndDate(request.getActualEndDate());
        }

        project = projectRepository.save(project);

        auditLogService.record("projects", projectCode, AuditAction.UPDATE, currentUserId,
                "Updated project metadata: " + project.getName());

        return mapToResponse(project);
    }

    /**
     * Archives a project. Builder owner only.
     */
    @Transactional
    public void deleteProject(String projectCode) {
        Project project = projectAccessService.validateProjectOwner(projectCode);
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        project.setStatus(ProjectStatus.ARCHIVED);
        projectRepository.save(project);

        auditLogService.record("projects", projectCode, AuditAction.DELETE, currentUserId,
                "Archived project: " + projectCode);
    }

    private String generateUniqueProjectCode() {
        for (int i = 0; i < 20; i++) {
            StringBuilder sb = new StringBuilder("PRJ-");
            for (int j = 0; j < 6; j++) {
                sb.append(CODE_CHARS.charAt(secureRandom.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (!projectRepository.existsByProjectCode(code)) {
                return code;
            }
        }
        return "PRJ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .projectCode(project.getProjectCode())
                .name(project.getName())
                .description(project.getDescription())
                .location(project.getLocation())
                .status(project.getStatus())
                .plannedStartDate(project.getPlannedStartDate())
                .plannedEndDate(project.getPlannedEndDate())
                .actualStartDate(project.getActualStartDate())
                .actualEndDate(project.getActualEndDate())
                .createdById(project.getCreatedBy().getId())
                .createdByName(project.getCreatedBy().getFullName() != null ? project.getCreatedBy().getFullName() : project.getCreatedBy().getEmail())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
