package com.construction.service;

import com.construction.dto.progress.CreateProgressRequest;
import com.construction.dto.progress.ProgressResponse;
import com.construction.entity.Profile;
import com.construction.entity.Project;
import com.construction.entity.ProgressUpdate;
import com.construction.entity.enums.AccountStatus;
import com.construction.exception.ResourceNotFoundException;
import com.construction.repository.ProfileRepository;
import com.construction.repository.ProgressUpdateRepository;
import com.construction.repository.ProjectPhaseRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private ProgressUpdateRepository progressUpdateRepository;

    @Mock
    private ProjectPhaseRepository projectPhaseRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ProgressService progressService;

    private UUID userId;
    private Project project;
    private Profile profile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        profile = Profile.builder().id(userId).email("contractor@test.com").fullName("Contractor Bob").status(AccountStatus.ACTIVE).build();

        project = Project.builder()
                .id(UUID.randomUUID())
                .projectCode("PRJ-PROG1")
                .name("Progress Project")
                .build();

        UserPrincipal principal = UserPrincipal.builder()
                .id(userId)
                .email("contractor@test.com")
                .status(AccountStatus.ACTIVE)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CONTRACTOR")))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Create progress update persists data and records audit log")
    void createProgressUpdate_Success() {
        CreateProgressRequest request = CreateProgressRequest.builder()
                .percentageCompleted(new BigDecimal("45.50"))
                .title("Foundation Pouring Complete")
                .description("Concrete poured and cured on Block A")
                .workCompletedToday("Poured 120 cubic yards of concrete")
                .hasBlockers(false)
                .updateDate(LocalDate.now())
                .build();

        when(projectAccessService.validateProjectWriteAccess("PRJ-PROG1")).thenReturn(project);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(progressUpdateRepository.save(any(ProgressUpdate.class))).thenAnswer(i -> {
            ProgressUpdate p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(LocalDateTime.now());
            return p;
        });

        ProgressResponse response = progressService.createProgressUpdate("PRJ-PROG1", request);

        assertNotNull(response);
        assertEquals(new BigDecimal("45.50"), response.getPercentageCompleted());
        assertEquals("Foundation Pouring Complete", response.getTitle());
        assertEquals(userId, response.getSubmittedById());
        assertFalse(response.isHasBlockers());

        verify(auditLogService).record(eq("progress_updates"), anyString(), any(), eq(userId), anyString());
    }

    @Test
    @DisplayName("Get latest progress update returns most recent entry")
    void getLatestProgressUpdate_Success() {
        ProgressUpdate latest = ProgressUpdate.builder()
                .id(UUID.randomUUID())
                .project(project)
                .submittedBy(profile)
                .percentageCompleted(new BigDecimal("70.00"))
                .title("Framing Complete")
                .hasBlockers(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(projectAccessService.validateProjectReadAccess("PRJ-PROG1")).thenReturn(project);
        when(progressUpdateRepository.findFirstByProject_ProjectCodeOrderByCreatedAtDesc("PRJ-PROG1"))
                .thenReturn(Optional.of(latest));

        ProgressResponse response = progressService.getLatestProgressUpdate("PRJ-PROG1");

        assertNotNull(response);
        assertEquals(new BigDecimal("70.00"), response.getPercentageCompleted());
        assertEquals("Framing Complete", response.getTitle());
    }

    @Test
    @DisplayName("Valid daily construction area submission stores category, area, and unit")
    void createProgressUpdate_ValidDailyAreaSubmission_Success() {
        CreateProgressRequest request = CreateProgressRequest.builder()
                .workCategory("BRICKWORK")
                .areaCompleted(new BigDecimal("250.00"))
                .areaUnit("SQFT")
                .percentageCompleted(new BigDecimal("35.00"))
                .title("First floor brickwork progress")
                .description("Brickwork completed on first floor.")
                .workCompletedToday("250 SQFT of brickwork completed.")
                .hasBlockers(false)
                .updateDate(LocalDate.now())
                .build();

        when(projectAccessService.validateProjectWriteAccess("PRJ-PROG1")).thenReturn(project);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(progressUpdateRepository.save(any(ProgressUpdate.class))).thenAnswer(i -> {
            ProgressUpdate p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(LocalDateTime.now());
            return p;
        });

        ProgressResponse response = progressService.createProgressUpdate("PRJ-PROG1", request);

        assertNotNull(response);
        assertEquals("BRICKWORK", response.getWorkCategory());
        assertEquals(new BigDecimal("250.00"), response.getAreaCompleted());
        assertEquals("SQFT", response.getAreaUnit());
        assertEquals(new BigDecimal("35.00"), response.getPercentageCompleted());
        assertEquals("contractor@test.com", response.getSubmittedByEmail());
    }

    @Test
    @DisplayName("Negative area rejection throws BadRequestException")
    void createProgressUpdate_NegativeArea_ThrowsBadRequest() {
        CreateProgressRequest request = CreateProgressRequest.builder()
                .workCategory("PLASTERING")
                .areaCompleted(new BigDecimal("-50.00"))
                .areaUnit("SQFT")
                .percentageCompleted(new BigDecimal("35.00"))
                .title("Invalid negative area progress")
                .build();

        when(projectAccessService.validateProjectWriteAccess("PRJ-PROG1")).thenReturn(project);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        com.construction.exception.BadRequestException ex = assertThrows(
                com.construction.exception.BadRequestException.class,
                () -> progressService.createProgressUpdate("PRJ-PROG1", request)
        );

        assertTrue(ex.getMessage().contains("cannot be negative"));
        verify(progressUpdateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Invalid measurement unit throws BadRequestException")
    void createProgressUpdate_InvalidUnit_ThrowsBadRequest() {
        CreateProgressRequest request = CreateProgressRequest.builder()
                .workCategory("FLOORING")
                .areaCompleted(new BigDecimal("100.00"))
                .areaUnit("MILES")
                .percentageCompleted(new BigDecimal("35.00"))
                .title("Invalid unit progress")
                .build();

        when(projectAccessService.validateProjectWriteAccess("PRJ-PROG1")).thenReturn(project);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        com.construction.exception.BadRequestException ex = assertThrows(
                com.construction.exception.BadRequestException.class,
                () -> progressService.createProgressUpdate("PRJ-PROG1", request)
        );

        assertTrue(ex.getMessage().contains("Invalid measurement unit"));
        verify(progressUpdateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Date-wise progress retrieval returns updates for requested date")
    void getProgressUpdatesByDate_Success() {
        LocalDate today = LocalDate.now();
        ProgressUpdate update = ProgressUpdate.builder()
                .id(UUID.randomUUID())
                .project(project)
                .submittedBy(profile)
                .workCategory("CONCRETE_POURING")
                .areaCompleted(new BigDecimal("500.00"))
                .areaUnit("SQFT")
                .percentageCompleted(new BigDecimal("50.00"))
                .title("Slab Casting")
                .updateDate(today)
                .createdAt(LocalDateTime.now())
                .build();

        when(projectAccessService.validateProjectReadAccess("PRJ-PROG1")).thenReturn(project);
        when(progressUpdateRepository.findByProject_ProjectCodeAndUpdateDateOrderByCreatedAtDesc("PRJ-PROG1", today))
                .thenReturn(List.of(update));

        List<ProgressResponse> responses = progressService.getProgressUpdatesByDate("PRJ-PROG1", today);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("CONCRETE_POURING", responses.get(0).getWorkCategory());
        assertEquals(today, responses.get(0).getUpdateDate());
    }
}
