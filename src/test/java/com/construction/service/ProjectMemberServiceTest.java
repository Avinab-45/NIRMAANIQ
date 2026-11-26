package com.construction.service;

import com.construction.dto.member.AddMemberRequest;
import com.construction.dto.member.ProjectMemberResponse;
import com.construction.entity.Profile;
import com.construction.entity.Project;
import com.construction.entity.ProjectMember;
import com.construction.entity.Role;
import com.construction.entity.enums.AccountStatus;
import com.construction.entity.enums.MemberStatus;
import com.construction.entity.enums.ProjectMemberRole;
import com.construction.entity.enums.RoleType;
import com.construction.exception.BadRequestException;
import com.construction.exception.ResourceNotFoundException;
import com.construction.repository.ProfileRepository;
import com.construction.repository.ProjectMemberRepository;
import com.construction.repository.RoleRepository;
import com.construction.repository.UserRoleRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    private UUID builderId;
    private Project project;

    @BeforeEach
    void setUp() {
        builderId = UUID.randomUUID();
        Profile builderProfile = Profile.builder().id(builderId).email("builder@test.com").status(AccountStatus.ACTIVE).build();

        project = Project.builder()
                .id(UUID.randomUUID())
                .projectCode("PRJ-MEMBER1")
                .name("Member Test Project")
                .createdBy(builderProfile)
                .build();

        UserPrincipal principal = UserPrincipal.builder()
                .id(builderId)
                .email("builder@test.com")
                .status(AccountStatus.ACTIVE)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_BUILDER")))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Assign contractor by verified email adds member (acts as contractor invitation)")
    void addMember_ContractorByEmail_Success() {
        String email = "contractor@build.com";
        UUID contractorId = UUID.randomUUID();
        Profile contractorProfile = Profile.builder().id(contractorId).email(email).status(AccountStatus.ACTIVE).build();

        when(projectAccessService.validateProjectOwner("PRJ-MEMBER1")).thenReturn(project);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.of(contractorProfile));
        when(userRoleRepository.existsByUser_IdAndRole_Name(contractorId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(contractorId, RoleType.ROLE_CLIENT)).thenReturn(false);
        when(projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(project.getId(), contractorId, MemberStatus.ACTIVE))
                .thenReturn(false);
        when(projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), contractorId))
                .thenReturn(Optional.empty());
        when(projectMemberRepository.save(any(ProjectMember.class))).thenAnswer(i -> {
            ProjectMember m = i.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        AddMemberRequest request = AddMemberRequest.builder()
                .role(ProjectMemberRole.CONTRACTOR)
                .email(email)
                .build();

        ProjectMemberResponse response = projectMemberService.addMember("PRJ-MEMBER1", request);

        assertNotNull(response);
        assertEquals(contractorId, response.getUserId());
        assertEquals(ProjectMemberRole.CONTRACTOR, response.getRole());
        assertEquals(MemberStatus.ACTIVE, response.getStatus());

        verify(notificationService).createNotification(eq(contractorId), eq(project), anyString(), anyString(), any());
        verify(auditLogService).record(eq("project_members"), anyString(), any(), eq(builderId), anyString());
    }

    @Test
    @DisplayName("Assign contractor who is registered as Builder throws BadRequestException")
    void addMember_ContractorWithBuilderRole_ThrowsBadRequest() {
        String email = "builder.contractor@build.com";
        UUID contractorId = UUID.randomUUID();
        Profile builderProfile = Profile.builder().id(contractorId).email(email).status(AccountStatus.ACTIVE).build();

        when(projectAccessService.validateProjectOwner("PRJ-MEMBER1")).thenReturn(project);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.of(builderProfile));
        when(userRoleRepository.existsByUser_IdAndRole_Name(contractorId, RoleType.ROLE_BUILDER)).thenReturn(true);

        AddMemberRequest request = AddMemberRequest.builder()
                .role(ProjectMemberRole.CONTRACTOR)
                .email(email)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> projectMemberService.addMember("PRJ-MEMBER1", request));
        assertTrue(ex.getMessage().contains("registered as a Builder"));
    }

    @Test
    @DisplayName("Assign contractor who is registered as Client throws BadRequestException")
    void addMember_ContractorWithClientRole_ThrowsBadRequest() {
        String email = "client.contractor@build.com";
        UUID contractorId = UUID.randomUUID();
        Profile clientProfile = Profile.builder().id(contractorId).email(email).status(AccountStatus.ACTIVE).build();

        when(projectAccessService.validateProjectOwner("PRJ-MEMBER1")).thenReturn(project);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.of(clientProfile));
        when(userRoleRepository.existsByUser_IdAndRole_Name(contractorId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(contractorId, RoleType.ROLE_CLIENT)).thenReturn(true);

        AddMemberRequest request = AddMemberRequest.builder()
                .role(ProjectMemberRole.CONTRACTOR)
                .email(email)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> projectMemberService.addMember("PRJ-MEMBER1", request));
        assertTrue(ex.getMessage().contains("registered as a Client"));
    }

    @Test
    @DisplayName("Assign suspended contractor throws BadRequestException")
    void addMember_ContractorSuspended_ThrowsBadRequest() {
        String email = "suspended@build.com";
        UUID contractorId = UUID.randomUUID();
        Profile suspended = Profile.builder().id(contractorId).email(email).status(AccountStatus.SUSPENDED).build();

        when(projectAccessService.validateProjectOwner("PRJ-MEMBER1")).thenReturn(project);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.of(suspended));

        AddMemberRequest request = AddMemberRequest.builder()
                .role(ProjectMemberRole.CONTRACTOR)
                .email(email)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> projectMemberService.addMember("PRJ-MEMBER1", request));
        assertTrue(ex.getMessage().contains("suspended"));
    }

    @Test
    @DisplayName("Assign client by registered email assigns ROLE_CLIENT if missing and adds member")
    void addMember_ClientByEmail_Success() {
        String email = "client@clientdomain.com";
        UUID clientId = UUID.randomUUID();
        Profile clientProfile = Profile.builder().id(clientId).email(email).status(AccountStatus.ACTIVE).build();

        Role clientRole = Role.builder().id(UUID.randomUUID()).name(RoleType.ROLE_CLIENT).build();

        when(projectAccessService.validateProjectOwner("PRJ-MEMBER1")).thenReturn(project);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.of(clientProfile));
        when(userRoleRepository.existsByUser_IdAndRole_Name(clientId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(clientId, RoleType.ROLE_CONTRACTOR)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(clientId, RoleType.ROLE_CLIENT)).thenReturn(false);
        when(roleRepository.findByName(RoleType.ROLE_CLIENT)).thenReturn(Optional.of(clientRole));
        when(projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(project.getId(), clientId, MemberStatus.ACTIVE))
                .thenReturn(false);
        when(projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), clientId))
                .thenReturn(Optional.empty());
        when(projectMemberRepository.save(any(ProjectMember.class))).thenAnswer(i -> {
            ProjectMember m = i.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        AddMemberRequest request = AddMemberRequest.builder()
                .role(ProjectMemberRole.CLIENT)
                .email(email)
                .phoneNumber("+919876543210")
                .build();

        ProjectMemberResponse response = projectMemberService.addMember("PRJ-MEMBER1", request);

        assertNotNull(response);
        assertEquals(clientId, response.getUserId());
        assertEquals(ProjectMemberRole.CLIENT, response.getRole());
        assertEquals(MemberStatus.ACTIVE, response.getStatus());

        verify(userRoleRepository).save(any());
        verify(profileRepository).save(clientProfile);
        assertEquals("+919876543210", clientProfile.getPhoneNumber());
        verify(notificationService).createNotification(eq(clientId), eq(project), anyString(), anyString(), any());
        verify(auditLogService).record(eq("project_members"), anyString(), any(), eq(builderId), anyString());
    }

    @Test
    @DisplayName("Assign client with unregistered email throws ResourceNotFoundException")
    void addMember_Client_UnregisteredEmail_ThrowsNotFound() {
        String email = "unknown@client.com";

        when(projectAccessService.validateProjectOwner("PRJ-MEMBER1")).thenReturn(project);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.empty());

        AddMemberRequest request = AddMemberRequest.builder()
                .role(ProjectMemberRole.CLIENT)
                .email(email)
                .build();

        assertThrows(ResourceNotFoundException.class, () -> projectMemberService.addMember("PRJ-MEMBER1", request));
    }

    @Test
    @DisplayName("Assign client with suspended account throws BadRequestException")
    void addMember_Client_Suspended_ThrowsBadRequest() {
        String email = "suspended.client@test.com";
        UUID clientId = UUID.randomUUID();
        Profile suspendedClient = Profile.builder().id(clientId).email(email).status(AccountStatus.SUSPENDED).build();

        when(projectAccessService.validateProjectOwner("PRJ-MEMBER1")).thenReturn(project);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.of(suspendedClient));

        AddMemberRequest request = AddMemberRequest.builder()
                .role(ProjectMemberRole.CLIENT)
                .email(email)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> projectMemberService.addMember("PRJ-MEMBER1", request));
        assertTrue(ex.getMessage().contains("suspended"));
    }

    @Test
    @DisplayName("Assign client who has ROLE_BUILDER throws BadRequestException")
    void addMember_Client_HasBuilderRole_ThrowsBadRequest() {
        String email = "builder.client@test.com";
        UUID clientId = UUID.randomUUID();
        Profile builderProfile = Profile.builder().id(clientId).email(email).status(AccountStatus.ACTIVE).build();

        when(projectAccessService.validateProjectOwner("PRJ-MEMBER1")).thenReturn(project);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.of(builderProfile));
        when(userRoleRepository.existsByUser_IdAndRole_Name(clientId, RoleType.ROLE_BUILDER)).thenReturn(true);

        AddMemberRequest request = AddMemberRequest.builder()
                .role(ProjectMemberRole.CLIENT)
                .email(email)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> projectMemberService.addMember("PRJ-MEMBER1", request));
        assertTrue(ex.getMessage().contains("Builder"));
    }

    @Test
    @DisplayName("Assign client who has ROLE_CONTRACTOR throws BadRequestException")
    void addMember_Client_HasContractorRole_ThrowsBadRequest() {
        String email = "contractor.client@test.com";
        UUID clientId = UUID.randomUUID();
        Profile contractorProfile = Profile.builder().id(clientId).email(email).status(AccountStatus.ACTIVE).build();

        when(projectAccessService.validateProjectOwner("PRJ-MEMBER1")).thenReturn(project);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.of(contractorProfile));
        when(userRoleRepository.existsByUser_IdAndRole_Name(clientId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(clientId, RoleType.ROLE_CONTRACTOR)).thenReturn(true);

        AddMemberRequest request = AddMemberRequest.builder()
                .role(ProjectMemberRole.CLIENT)
                .email(email)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> projectMemberService.addMember("PRJ-MEMBER1", request));
        assertTrue(ex.getMessage().contains("Contractor"));
    }

    @Test
    @DisplayName("Assign client when already active member throws BadRequestException")
    void addMember_Client_AlreadyActiveMember_ThrowsBadRequest() {
        String email = "client@clientdomain.com";
        UUID clientId = UUID.randomUUID();
        Profile clientProfile = Profile.builder().id(clientId).email(email).status(AccountStatus.ACTIVE).build();

        when(projectAccessService.validateProjectOwner("PRJ-MEMBER1")).thenReturn(project);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.of(clientProfile));
        when(userRoleRepository.existsByUser_IdAndRole_Name(clientId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(clientId, RoleType.ROLE_CONTRACTOR)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(clientId, RoleType.ROLE_CLIENT)).thenReturn(true);
        when(projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(project.getId(), clientId, MemberStatus.ACTIVE))
                .thenReturn(true);

        AddMemberRequest request = AddMemberRequest.builder()
                .role(ProjectMemberRole.CLIENT)
                .email(email)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> projectMemberService.addMember("PRJ-MEMBER1", request));
        assertTrue(ex.getMessage().contains("already an active member"));
    }

    @Test
    @DisplayName("Assign member with null or blank email throws BadRequestException")
    void addMember_MissingEmail_ThrowsBadRequest() {
        when(projectAccessService.validateProjectOwner("PRJ-MEMBER1")).thenReturn(project);

        AddMemberRequest request = AddMemberRequest.builder()
                .role(ProjectMemberRole.CLIENT)
                .email("   ")
                .build();

        assertThrows(BadRequestException.class, () -> projectMemberService.addMember("PRJ-MEMBER1", request));
    }

    @Test
    @DisplayName("Remove member sets status to INACTIVE")
    void removeMember_Success() {
        UUID memberUserId = UUID.randomUUID();
        ProjectMember member = ProjectMember.builder()
                .id(UUID.randomUUID())
                .project(project)
                .user(Profile.builder().id(memberUserId).email("sub@test.com").build())
                .role(ProjectMemberRole.CONTRACTOR)
                .status(MemberStatus.ACTIVE)
                .build();

        when(projectAccessService.validateProjectOwner("PRJ-MEMBER1")).thenReturn(project);
        when(projectMemberRepository.findByProject_ProjectCodeAndUser_Id("PRJ-MEMBER1", memberUserId))
                .thenReturn(Optional.of(member));

        projectMemberService.removeMember("PRJ-MEMBER1", memberUserId);

        assertEquals(MemberStatus.INACTIVE, member.getStatus());
        verify(projectMemberRepository).save(member);
    }
}
