package com.construction.service;

import com.construction.dto.project.CreateProjectRequest;
import com.construction.dto.project.ProjectResponse;
import com.construction.dto.project.UpdateProjectRequest;
import com.construction.entity.Profile;
import com.construction.entity.Project;
import com.construction.entity.ProjectMember;
import com.construction.entity.enums.AccountStatus;
import com.construction.entity.enums.ProjectMemberRole;
import com.construction.entity.enums.ProjectStatus;
import com.construction.entity.enums.RoleType;
import com.construction.repository.ProfileRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ProjectService projectService;

    private UUID builderId;
    private Profile builderProfile;

    @BeforeEach
    void setUp() {
        builderId = UUID.randomUUID();
        builderProfile = Profile.builder()
                .id(builderId)
                .email("builder@test.com")
                .fullName("Master Builder")
                .status(AccountStatus.ACTIVE)
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
    @DisplayName("Create project auto-generates unique project code and assigns builder as OWNER in project_members")
    void createProject_Success() {
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("Greenfield Residency")
                .description("Luxury apartments")
                .location("Sector 42")
                .plannedStartDate(LocalDate.now())
                .plannedEndDate(LocalDate.now().plusMonths(12))
                .build();

        when(profileRepository.findById(builderId)).thenReturn(Optional.of(builderProfile));
        when(projectRepository.existsByProjectCode(anyString())).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> {
            Project p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        ProjectResponse response = projectService.createProject(request);

        assertNotNull(response);
        assertNotNull(response.getProjectCode());
        assertTrue(response.getProjectCode().startsWith("PRJ-"));
        assertEquals("Greenfield Residency", response.getName());
        assertEquals(ProjectStatus.PLANNING, response.getStatus());

        verify(projectMemberRepository).save(argThat(member ->
                member.getRole() == ProjectMemberRole.OWNER &&
                member.getUser().getId().equals(builderId)
        ));
        verify(auditLogService).record(eq("projects"), anyString(), any(), eq(builderId), anyString());
    }

    @Test
    @DisplayName("Update project modifies metadata successfully")
    void updateProject_Success() {
        Project existingProject = Project.builder()
                .id(UUID.randomUUID())
                .projectCode("PRJ-EXISTING")
                .name("Old Name")
                .status(ProjectStatus.PLANNING)
                .createdBy(builderProfile)
                .build();

        when(projectAccessService.validateProjectOwner("PRJ-EXISTING")).thenReturn(existingProject);
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArgument(0));

        UpdateProjectRequest updateReq = UpdateProjectRequest.builder()
                .name("New Name")
                .status(ProjectStatus.IN_PROGRESS)
                .build();

        ProjectResponse response = projectService.updateProject("PRJ-EXISTING", updateReq);

        assertNotNull(response);
        assertEquals("New Name", response.getName());
        assertEquals(ProjectStatus.IN_PROGRESS, response.getStatus());
    }

    @Test
    @DisplayName("Delete project archives project")
    void deleteProject_ArchivesProject() {
        Project existingProject = Project.builder()
                .id(UUID.randomUUID())
                .projectCode("PRJ-EXISTING")
                .name("To Archive")
                .status(ProjectStatus.IN_PROGRESS)
                .createdBy(builderProfile)
                .build();

        when(projectAccessService.validateProjectOwner("PRJ-EXISTING")).thenReturn(existingProject);

        projectService.deleteProject("PRJ-EXISTING");

        assertEquals(ProjectStatus.ARCHIVED, existingProject.getStatus());
        verify(projectRepository).save(existingProject);
    }
}
