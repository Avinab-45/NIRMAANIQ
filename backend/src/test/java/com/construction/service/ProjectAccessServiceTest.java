package com.construction.service;

import com.construction.entity.Profile;
import com.construction.entity.Project;
import com.construction.entity.enums.AccountStatus;
import com.construction.entity.enums.MemberStatus;
import com.construction.entity.enums.ProjectStatus;
import com.construction.entity.enums.RoleType;
import com.construction.exception.ForbiddenException;
import com.construction.repository.ProjectMemberRepository;
import com.construction.repository.ProjectRepository;
import com.construction.security.ProjectAccessService;
import com.construction.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectAccessServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectAccessService projectAccessService;

    private UUID builderId;
    private UUID otherUserId;
    private Project project;

    @BeforeEach
    void setUp() {
        builderId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        Profile ownerProfile = Profile.builder().id(builderId).email("owner@test.com").status(AccountStatus.ACTIVE).build();

        project = Project.builder()
                .id(UUID.randomUUID())
                .projectCode("PRJ-ABC123")
                .name("Tower Project")
                .status(ProjectStatus.IN_PROGRESS)
                .createdBy(ownerProfile)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(UUID id, RoleType role, AccountStatus status) {
        UserPrincipal principal = UserPrincipal.builder()
                .id(id)
                .email("user@test.com")
                .status(status)
                .authorities(List.of(new SimpleGrantedAuthority(role.name())))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    @DisplayName("Owner builder access succeeds for project owner")
    void validateProjectOwner_OwnerBuilder_Success() {
        authenticateUser(builderId, RoleType.ROLE_BUILDER, AccountStatus.ACTIVE);
        when(projectRepository.findByProjectCode("PRJ-ABC123")).thenReturn(Optional.of(project));

        Project result = projectAccessService.validateProjectOwner("PRJ-ABC123");
        assertNotNull(result);
        assertEquals("PRJ-ABC123", result.getProjectCode());
    }

    @Test
    @DisplayName("Owner builder access throws ForbiddenException for non-owner builder")
    void validateProjectOwner_NonOwnerBuilder_ThrowsForbidden() {
        authenticateUser(otherUserId, RoleType.ROLE_BUILDER, AccountStatus.ACTIVE);
        when(projectRepository.findByProjectCode("PRJ-ABC123")).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProject_ProjectCodeAndUser_Id("PRJ-ABC123", otherUserId))
                .thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> projectAccessService.validateProjectOwner("PRJ-ABC123"));
    }

    @Test
    @DisplayName("Write access succeeds for assigned contractor")
    void validateProjectWriteAccess_AssignedContractor_Success() {
        authenticateUser(otherUserId, RoleType.ROLE_CONTRACTOR, AccountStatus.ACTIVE);
        when(projectRepository.findByProjectCode("PRJ-ABC123")).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProject_ProjectCodeAndUser_IdAndStatus("PRJ-ABC123", otherUserId, MemberStatus.ACTIVE))
                .thenReturn(true);

        Project result = projectAccessService.validateProjectWriteAccess("PRJ-ABC123");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Client write access throws ForbiddenException (Client read-only restriction)")
    void validateProjectWriteAccess_Client_ThrowsForbidden() {
        authenticateUser(otherUserId, RoleType.ROLE_CLIENT, AccountStatus.ACTIVE);
        when(projectRepository.findByProjectCode("PRJ-ABC123")).thenReturn(Optional.of(project));

        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> projectAccessService.validateProjectWriteAccess("PRJ-ABC123"));
        assertTrue(exception.getMessage().contains("Clients have read-only access"));
    }

    @Test
    @DisplayName("Read access succeeds for assigned client")
    void validateProjectReadAccess_AssignedClient_Success() {
        authenticateUser(otherUserId, RoleType.ROLE_CLIENT, AccountStatus.ACTIVE);
        when(projectRepository.findByProjectCode("PRJ-ABC123")).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProject_ProjectCodeAndUser_IdAndStatus("PRJ-ABC123", otherUserId, MemberStatus.ACTIVE))
                .thenReturn(true);

        Project result = projectAccessService.validateProjectReadAccess("PRJ-ABC123");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Read access throws ForbiddenException for unassigned user")
    void validateProjectReadAccess_UnassignedUser_ThrowsForbidden() {
        authenticateUser(otherUserId, RoleType.ROLE_CONTRACTOR, AccountStatus.ACTIVE);
        when(projectRepository.findByProjectCode("PRJ-ABC123")).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProject_ProjectCodeAndUser_IdAndStatus("PRJ-ABC123", otherUserId, MemberStatus.ACTIVE))
                .thenReturn(false);

        assertThrows(ForbiddenException.class, () -> projectAccessService.validateProjectReadAccess("PRJ-ABC123"));
    }
}
