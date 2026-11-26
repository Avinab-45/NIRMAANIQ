package com.construction.service;

import com.construction.dto.member.AddMemberRequest;
import com.construction.dto.member.ProjectMemberResponse;
import com.construction.entity.Profile;
import com.construction.entity.Project;
import com.construction.entity.ProjectMember;
import com.construction.entity.Role;
import com.construction.entity.UserRole;
import com.construction.entity.enums.AccountStatus;
import com.construction.entity.enums.AuditAction;
import com.construction.entity.enums.MemberStatus;
import com.construction.entity.enums.NotificationType;
import com.construction.entity.enums.ProjectMemberRole;
import com.construction.entity.enums.RoleType;
import com.construction.exception.BadRequestException;
import com.construction.exception.ResourceNotFoundException;
import com.construction.repository.ProfileRepository;
import com.construction.repository.ProjectMemberRepository;
import com.construction.repository.RoleRepository;
import com.construction.repository.UserRoleRepository;
import com.construction.security.ProjectAccessService;
import com.construction.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ProjectAccessService projectAccessService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    /**
     * Builder assigns a member to a project using their registered email.
     * - If CONTRACTOR, verified email is checked for existing ROLE_CONTRACTOR.
     * - If CLIENT, verified email is checked for eligibility and assigned ROLE_CLIENT.
     */
    @Transactional
    public ProjectMemberResponse addMember(String projectCode, AddMemberRequest request) {
        Project project = projectAccessService.validateProjectOwner(projectCode);
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Registered email is required for member assignment.");
        }
        String email = request.getEmail().trim().toLowerCase();

        Profile memberProfile;

        if (request.getRole() == ProjectMemberRole.CONTRACTOR) {
            memberProfile = profileRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("No registered contractor found with email: " + email));

            if (memberProfile.getStatus() == AccountStatus.SUSPENDED) {
                throw new BadRequestException("Contractor account is suspended and ineligible for project assignment.");
            }

            // Verify account does not have conflicting roles
            if (userRoleRepository.existsByUser_IdAndRole_Name(memberProfile.getId(), RoleType.ROLE_BUILDER)) {
                throw new BadRequestException("User " + email + " is registered as a Builder and cannot be assigned as a Contractor.");
            }
            if (userRoleRepository.existsByUser_IdAndRole_Name(memberProfile.getId(), RoleType.ROLE_CLIENT)) {
                throw new BadRequestException("User " + email + " is registered as a Client and cannot be assigned as a Contractor.");
            }
        } else if (request.getRole() == ProjectMemberRole.CLIENT) {
            memberProfile = profileRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("No registered client found with email: " + email));

            // Verify eligibility: account must not be suspended
            if (memberProfile.getStatus() == AccountStatus.SUSPENDED) {
                throw new BadRequestException("Client account is suspended and ineligible for project assignment.");
            }

            // Verify eligibility: account must not have conflicting roles
            if (userRoleRepository.existsByUser_IdAndRole_Name(memberProfile.getId(), RoleType.ROLE_BUILDER)) {
                throw new BadRequestException("User " + email + " is registered as a Builder and cannot be assigned as a Client.");
            }
            if (userRoleRepository.existsByUser_IdAndRole_Name(memberProfile.getId(), RoleType.ROLE_CONTRACTOR)) {
                throw new BadRequestException("User " + email + " is registered as a Contractor and cannot be assigned as a Client.");
            }

            // Ensure ROLE_CLIENT is assigned
            if (!userRoleRepository.existsByUser_IdAndRole_Name(memberProfile.getId(), RoleType.ROLE_CLIENT)) {
                Role clientRole = roleRepository.findByName(RoleType.ROLE_CLIENT)
                        .orElseGet(() -> roleRepository.save(Role.builder()
                                .name(RoleType.ROLE_CLIENT)
                                .description("Client with read-only access to assigned projects")
                                .build()));

                UserRole userRole = UserRole.builder()
                        .user(memberProfile)
                        .role(clientRole)
                        .assignedAt(LocalDateTime.now())
                        .build();
                userRoleRepository.save(userRole);
            }
        } else {
            throw new BadRequestException("Invalid assignment role: " + request.getRole() + ". Must be CONTRACTOR or CLIENT.");
        }

        // Check if already an active member
        if (projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(project.getId(), memberProfile.getId(), MemberStatus.ACTIVE)) {
            throw new BadRequestException("User is already an active member of this project.");
        }

        ProjectMember member = projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), memberProfile.getId())
                .orElse(ProjectMember.builder()
                        .project(project)
                        .user(memberProfile)
                        .role(request.getRole())
                        .status(MemberStatus.ACTIVE)
                        .joinedAt(LocalDateTime.now())
                        .build());

        member.setStatus(MemberStatus.ACTIVE);
        member.setRole(request.getRole());
        member = projectMemberRepository.save(member);

        // Update optional contact phone if supplied and not already present
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank() && memberProfile.getPhoneNumber() == null) {
            memberProfile.setPhoneNumber(request.getPhoneNumber().trim());
            profileRepository.save(memberProfile);
        }

        // Notify member
        notificationService.createNotification(
                memberProfile.getId(),
                project,
                "Assigned to Project: " + project.getName(),
                "You have been assigned as a " + request.getRole() + " to project " + project.getName() + " (" + projectCode + ").",
                NotificationType.INVITATION
        );

        auditLogService.record("project_members", member.getId().toString(), AuditAction.CREATE, currentUserId,
                "Assigned " + request.getRole() + " (" + memberProfile.getEmail() + ") to project " + projectCode);

        return mapToResponse(member);
    }

    /**
     * Lists all members of a project.
     * Accessible by project owner or assigned members.
     */
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getMembers(String projectCode) {
        projectAccessService.validateProjectReadAccess(projectCode);
        return projectMemberRepository.findByProject_ProjectCode(projectCode)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Removes a member from a project. Builder owner only.
     */
    @Transactional
    public void removeMember(String projectCode, UUID userId) {
        projectAccessService.validateProjectOwner(projectCode);
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        ProjectMember member = projectMemberRepository.findByProject_ProjectCodeAndUser_Id(projectCode, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in project: " + projectCode));

        if (member.getRole() == ProjectMemberRole.OWNER) {
            throw new BadRequestException("Cannot remove the project owner.");
        }

        member.setStatus(MemberStatus.INACTIVE);
        projectMemberRepository.save(member);

        auditLogService.record("project_members", member.getId().toString(), AuditAction.DELETE, currentUserId,
                "Removed member " + userId + " from project " + projectCode);
    }

    private ProjectMemberResponse mapToResponse(ProjectMember m) {
        return ProjectMemberResponse.builder()
                .id(m.getId())
                .userId(m.getUser().getId())
                .userEmail(m.getUser().getEmail())
                .userFullName(m.getUser().getFullName())
                .userPhone(m.getUser().getPhoneNumber())
                .role(m.getRole())
                .status(m.getStatus())
                .joinedAt(m.getJoinedAt())
                .build();
    }
}
