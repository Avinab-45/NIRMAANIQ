package com.construction.security;

import com.construction.entity.Project;
import com.construction.entity.enums.AccountStatus;
import com.construction.entity.enums.MemberStatus;
import com.construction.entity.enums.ProjectMemberRole;
import com.construction.entity.enums.RoleType;
import com.construction.exception.ForbiddenException;
import com.construction.exception.ResourceNotFoundException;
import com.construction.repository.ProjectMemberRepository;
import com.construction.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Enforces project-level security and prevents IDOR (Insecure Direct Object Reference)
 * by verifying authenticated user identity, active status, role, and project membership.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    /**
     * Verifies that the current user is an active BUILDER and the owner of the project.
     * Used for project administration, adding/removing members, creating budgets/phases.
     */
    @Transactional(readOnly = true)
    public Project validateProjectOwner(String projectCode) {
        UserPrincipal user = SecurityUtil.getCurrentUser();
        requireActiveUser(user);

        Project project = getProjectByCode(projectCode);

        // Check if user has BUILDER role
        if (!SecurityUtil.hasRole(RoleType.ROLE_BUILDER)) {
            log.warn("User {} attempted builder action without ROLE_BUILDER", user.getId());
            throw new ForbiddenException("Only builders can manage project configurations and members.");
        }

        // Verify project ownership
        if (!project.getCreatedBy().getId().equals(user.getId())) {
            // Also check project_members table for OWNER role
            boolean isOwnerMember = projectMemberRepository
                    .findByProject_ProjectCodeAndUser_Id(projectCode, user.getId())
                    .filter(m -> m.getRole() == ProjectMemberRole.OWNER && m.getStatus() == MemberStatus.ACTIVE)
                    .isPresent();

            if (!isOwnerMember) {
                log.warn("Builder {} attempted unauthorized management of project {}", user.getId(), projectCode);
                throw new ForbiddenException("You do not own this project.");
            }
        }

        return project;
    }

    /**
     * Verifies that the current user has WRITE permissions on the project.
     * Allowed: Builder owner, or assigned active Contractor.
     * Denied: Client (read-only), unassigned users, suspended users.
     */
    @Transactional(readOnly = true)
    public Project validateProjectWriteAccess(String projectCode) {
        UserPrincipal user = SecurityUtil.getCurrentUser();
        requireActiveUser(user);

        Project project = getProjectByCode(projectCode);

        // If Builder owns the project, they have write access
        if (SecurityUtil.hasRole(RoleType.ROLE_BUILDER) && project.getCreatedBy().getId().equals(user.getId())) {
            return project;
        }

        // If Contractor, verify active membership in this project
        if (SecurityUtil.hasRole(RoleType.ROLE_CONTRACTOR)) {
            boolean isAssigned = projectMemberRepository
                    .existsByProject_ProjectCodeAndUser_IdAndStatus(projectCode, user.getId(), MemberStatus.ACTIVE);
            if (isAssigned) {
                return project;
            }
        }

        // If Client, reject (Clients have strict read-only access)
        if (SecurityUtil.hasRole(RoleType.ROLE_CLIENT)) {
            throw new ForbiddenException("Clients have read-only access and cannot modify project records.");
        }

        log.warn("User {} attempted write operation on project {} without assignment", user.getId(), projectCode);
        throw new ForbiddenException("You are not an assigned active member with write permissions on this project.");
    }

    /**
     * Verifies that the current user has READ permissions on the project.
     * Allowed: Builder owner, assigned Contractor, or assigned Client.
     * Denied: Unassigned users, suspended users.
     */
    @Transactional(readOnly = true)
    public Project validateProjectReadAccess(String projectCode) {
        UserPrincipal user = SecurityUtil.getCurrentUser();
        requireActiveUser(user);

        Project project = getProjectByCode(projectCode);

        // If Builder owns the project, allow read
        if (SecurityUtil.hasRole(RoleType.ROLE_BUILDER) && project.getCreatedBy().getId().equals(user.getId())) {
            return project;
        }

        // Check if user is an active member (Contractor or Client)
        boolean isMember = projectMemberRepository
                .existsByProject_ProjectCodeAndUser_IdAndStatus(projectCode, user.getId(), MemberStatus.ACTIVE);

        if (isMember) {
            return project;
        }

        log.warn("User {} attempted read on project {} without assignment", user.getId(), projectCode);
        throw new ForbiddenException("Access denied: You are not assigned to project " + projectCode);
    }

    private Project getProjectByCode(String projectCode) {
        return projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with code: " + projectCode));
    }

    private void requireActiveUser(UserPrincipal user) {
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenException("Account is not active. Status: " + user.getStatus());
        }
    }
}
