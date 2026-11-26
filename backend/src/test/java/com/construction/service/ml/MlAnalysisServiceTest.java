package com.construction.service.ml;

import com.construction.config.MlProperties;
import com.construction.dto.ai.MediaAiAnalysisResponse;
import com.construction.dto.ml.FastApiPredictResponse;
import com.construction.dto.ml.MlDetection;
import com.construction.entity.MediaAiAnalysis;
import com.construction.entity.MediaFile;
import com.construction.entity.Profile;
import com.construction.entity.Project;
import com.construction.entity.enums.AccountStatus;
import com.construction.entity.enums.AiAnalysisStatus;
import com.construction.entity.enums.MediaCategory;
import com.construction.exception.BadRequestException;
import com.construction.exception.MlServiceException;
import com.construction.exception.ResourceNotFoundException;
import com.construction.repository.MediaAiAnalysisRepository;
import com.construction.repository.MediaFileRepository;
import com.construction.repository.ProfileRepository;
import com.construction.security.ProjectAccessService;
import com.construction.security.UserPrincipal;
import com.construction.service.AuditLogService;
import com.construction.service.SupabaseStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MlAnalysisServiceTest {

    @Mock
    private MediaFileRepository mediaFileRepository;

    @Mock
    private MediaAiAnalysisRepository mediaAiAnalysisRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private SupabaseStorageService supabaseStorageService;

    @Mock
    private MlServiceClient mlServiceClient;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private MlProperties mlProperties;

    @InjectMocks
    private MlAnalysisService mlAnalysisService;

    private UUID userId;
    private Project project;
    private Profile userProfile;
    private MediaFile imageMediaFile;
    private MediaFile documentMediaFile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userProfile = Profile.builder()
                .id(userId)
                .email("contractor@nirmaaniq.com")
                .fullName("Contractor User")
                .status(AccountStatus.ACTIVE)
                .build();

        project = Project.builder()
                .id(UUID.randomUUID())
                .projectCode("PRJ-YOLO11")
                .name("YOLO11 Test Site")
                .createdBy(userProfile)
                .build();

        imageMediaFile = MediaFile.builder()
                .id(UUID.randomUUID())
                .project(project)
                .uploadedBy(userProfile)
                .fileName("excavation_pit.jpg")
                .storagePath("PRJ-YOLO11/excavation_pit.jpg")
                .fileSize(1024L * 500)
                .mimeType("image/jpeg")
                .mediaCategory(MediaCategory.SITE_PHOTO)
                .createdAt(LocalDateTime.now())
                .build();

        documentMediaFile = MediaFile.builder()
                .id(UUID.randomUUID())
                .project(project)
                .uploadedBy(userProfile)
                .fileName("blueprint.pdf")
                .storagePath("PRJ-YOLO11/blueprint.pdf")
                .fileSize(1024L * 2000)
                .mimeType("application/pdf")
                .mediaCategory(MediaCategory.DOCUMENT)
                .createdAt(LocalDateTime.now())
                .build();

        UserPrincipal principal = UserPrincipal.builder()
                .id(userId)
                .email("contractor@nirmaaniq.com")
                .status(AccountStatus.ACTIVE)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CONTRACTOR")))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        lenient().when(mlProperties.isEnabled()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Successful YOLO11 analysis downloads private file, invokes FastAPI, persists COMPLETED status and audit record")
    void analyzeMediaFile_Success() {
        UUID mediaId = imageMediaFile.getId();

        when(projectAccessService.validateProjectWriteAccess("PRJ-YOLO11")).thenReturn(project);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(mediaFileRepository.findByIdAndProject_ProjectCode(mediaId, "PRJ-YOLO11"))
                .thenReturn(Optional.of(imageMediaFile));
        when(mediaAiAnalysisRepository.findByMediaFile_IdOrderByCreatedAtDesc(mediaId))
                .thenReturn(List.of());

        byte[] fakeBytes = "fake-jpg-binary".getBytes(StandardCharsets.UTF_8);
        when(supabaseStorageService.downloadFile(imageMediaFile.getStoragePath())).thenReturn(fakeBytes);
        when(supabaseStorageService.generateSignedUrl(imageMediaFile.getStoragePath()))
                .thenReturn("https://storage.supabase.co/signed-url");

        FastApiPredictResponse mockMlResponse = FastApiPredictResponse.builder()
                .model("yolo11x-construction")
                .version("1.0")
                .status("COMPLETED")
                .summary("Excavation trench inspected with visible safety barriers.")
                .workCategory("EXCAVATION")
                .estimatedCompletionPercentage(new BigDecimal("70.00"))
                .detections(List.of(
                        MlDetection.builder()
                                .className("safety_barrier")
                                .confidence(0.92)
                                .build()
                ))
                .issues(List.of("Water accumulation at perimeter"))
                .recommendations(List.of("Deploy dewatering pump"))
                .rawJson("{\"status\":\"COMPLETED\"}")
                .build();

        when(mlServiceClient.predict(eq(fakeBytes), eq("excavation_pit.jpg"), eq("image/jpeg"), eq("PRJ-YOLO11"), eq(mediaId)))
                .thenReturn(mockMlResponse);

        when(mediaAiAnalysisRepository.save(any(MediaAiAnalysis.class))).thenAnswer(i -> {
            MediaAiAnalysis a = i.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            return a;
        });

        MediaAiAnalysisResponse response = mlAnalysisService.analyzeMediaFile("PRJ-YOLO11", mediaId, false);

        assertNotNull(response);
        assertEquals(AiAnalysisStatus.COMPLETED, response.getStatus());
        assertEquals("YOLO11_FASTAPI", response.getAiProvider());
        assertEquals("yolo11x-construction:1.0", response.getAiModel());
        assertEquals("EXCAVATION", response.getDetectedWorkCategory());
        assertEquals(new BigDecimal("70.00"), response.getEstimatedCompletionPercentage());
        assertTrue(response.getAnalysisSummary().contains("Excavation trench"));
        assertTrue(response.getDetectedIssues().contains("Water accumulation"));
        assertTrue(response.getRecommendations().contains("dewatering pump"));
        assertNull(response.getErrorMessage());

        verify(mediaAiAnalysisRepository, atLeast(2)).save(any(MediaAiAnalysis.class));
        verify(auditLogService).record(eq("media_ai_analyses"), anyString(), any(), eq(userId), anyString());
    }

    @Test
    @DisplayName("Idempotency: returns cached COMPLETED analysis without re-downloading or calling FastAPI")
    void analyzeMediaFile_Idempotency_ReturnsCompletedCachedAnalysis() {
        UUID mediaId = imageMediaFile.getId();

        MediaAiAnalysis cachedAnalysis = MediaAiAnalysis.builder()
                .id(UUID.randomUUID())
                .mediaFile(imageMediaFile)
                .project(project)
                .analyzedBy(userProfile)
                .status(AiAnalysisStatus.COMPLETED)
                .aiProvider("YOLO11")
                .aiModel("yolo11x-construction")
                .analysisSummary("Already cached analysis")
                .createdAt(LocalDateTime.now().minusHours(1))
                .completedAt(LocalDateTime.now().minusHours(1))
                .build();

        when(projectAccessService.validateProjectWriteAccess("PRJ-YOLO11")).thenReturn(project);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(mediaFileRepository.findByIdAndProject_ProjectCode(mediaId, "PRJ-YOLO11"))
                .thenReturn(Optional.of(imageMediaFile));
        when(mediaAiAnalysisRepository.findByMediaFile_IdOrderByCreatedAtDesc(mediaId))
                .thenReturn(List.of(cachedAnalysis));
        when(supabaseStorageService.generateSignedUrl(imageMediaFile.getStoragePath()))
                .thenReturn("https://storage.supabase.co/signed-url");

        MediaAiAnalysisResponse response = mlAnalysisService.analyzeMediaFile("PRJ-YOLO11", mediaId, false);

        assertNotNull(response);
        assertEquals(cachedAnalysis.getId(), response.getId());
        assertEquals("Already cached analysis", response.getAnalysisSummary());

        verify(supabaseStorageService, never()).downloadFile(anyString());
        verifyNoInteractions(mlServiceClient);
    }

    @Test
    @DisplayName("reanalyze=true bypasses cached COMPLETED analysis and triggers fresh inference")
    void analyzeMediaFile_ForceReanalyze_BypassesCache() {
        UUID mediaId = imageMediaFile.getId();

        MediaAiAnalysis cachedAnalysis = MediaAiAnalysis.builder()
                .id(UUID.randomUUID())
                .mediaFile(imageMediaFile)
                .project(project)
                .analyzedBy(userProfile)
                .status(AiAnalysisStatus.COMPLETED)
                .aiProvider("YOLO11")
                .analysisSummary("Old analysis")
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();

        when(projectAccessService.validateProjectWriteAccess("PRJ-YOLO11")).thenReturn(project);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(mediaFileRepository.findByIdAndProject_ProjectCode(mediaId, "PRJ-YOLO11"))
                .thenReturn(Optional.of(imageMediaFile));
        when(mediaAiAnalysisRepository.findByMediaFile_IdOrderByCreatedAtDesc(mediaId))
                .thenReturn(List.of(cachedAnalysis));

        byte[] fakeBytes = "fresh-bytes".getBytes(StandardCharsets.UTF_8);
        when(supabaseStorageService.downloadFile(imageMediaFile.getStoragePath())).thenReturn(fakeBytes);
        when(supabaseStorageService.generateSignedUrl(imageMediaFile.getStoragePath()))
                .thenReturn("https://storage.supabase.co/signed-url");

        FastApiPredictResponse mockMlResponse = FastApiPredictResponse.builder()
                .model("yolo11x-construction")
                .status("COMPLETED")
                .summary("Fresh re-analysis summary")
                .build();
        when(mlServiceClient.predict(any(), any(), any(), any(), any())).thenReturn(mockMlResponse);
        when(mediaAiAnalysisRepository.save(any(MediaAiAnalysis.class))).thenAnswer(i -> {
            MediaAiAnalysis a = i.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            return a;
        });

        MediaAiAnalysisResponse response = mlAnalysisService.analyzeMediaFile("PRJ-YOLO11", mediaId, true);

        assertNotNull(response);
        assertEquals("Fresh re-analysis summary", response.getAnalysisSummary());
        verify(mlServiceClient).predict(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("FastAPI failure sets status to FAILED, persists error details, and never deletes photo from storage")
    void analyzeMediaFile_MlClientFailure_PersistsFailedStatus_PreservesPhoto() {
        UUID mediaId = imageMediaFile.getId();

        when(projectAccessService.validateProjectWriteAccess("PRJ-YOLO11")).thenReturn(project);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(mediaFileRepository.findByIdAndProject_ProjectCode(mediaId, "PRJ-YOLO11"))
                .thenReturn(Optional.of(imageMediaFile));
        when(mediaAiAnalysisRepository.findByMediaFile_IdOrderByCreatedAtDesc(mediaId))
                .thenReturn(List.of());

        byte[] fakeBytes = "fake-jpg".getBytes(StandardCharsets.UTF_8);
        when(supabaseStorageService.downloadFile(imageMediaFile.getStoragePath())).thenReturn(fakeBytes);
        when(supabaseStorageService.generateSignedUrl(imageMediaFile.getStoragePath()))
                .thenReturn("https://storage.supabase.co/signed-url");

        when(mlServiceClient.predict(any(), any(), any(), any(), any()))
                .thenThrow(new MlServiceException("FastAPI connection timeout", HttpStatus.GATEWAY_TIMEOUT));

        when(mediaAiAnalysisRepository.save(any(MediaAiAnalysis.class))).thenAnswer(i -> {
            MediaAiAnalysis a = i.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            return a;
        });

        MediaAiAnalysisResponse response = mlAnalysisService.analyzeMediaFile("PRJ-YOLO11", mediaId, false);

        assertNotNull(response);
        assertEquals(AiAnalysisStatus.FAILED, response.getStatus());
        assertTrue(response.getErrorMessage().contains("FastAPI connection timeout"));

        // Verify photo is preserved and NOT deleted
        verify(mediaFileRepository, never()).delete(any());
        verify(supabaseStorageService, never()).deleteFile(any());
    }

    @Test
    @DisplayName("Attempting to analyze non-image file throws BadRequestException")
    void analyzeMediaFile_UnsupportedMimeType_ThrowsBadRequest() {
        UUID mediaId = documentMediaFile.getId();

        when(projectAccessService.validateProjectWriteAccess("PRJ-YOLO11")).thenReturn(project);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(mediaFileRepository.findByIdAndProject_ProjectCode(mediaId, "PRJ-YOLO11"))
                .thenReturn(Optional.of(documentMediaFile));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                mlAnalysisService.analyzeMediaFile("PRJ-YOLO11", mediaId, false)
        );

        assertTrue(ex.getMessage().contains("only supported for image files"));
        verifyNoInteractions(mlServiceClient);
    }

    @Test
    @DisplayName("getLatestAnalysis returns most recent analysis for media file")
    void getLatestAnalysis_Success() {
        UUID mediaId = imageMediaFile.getId();
        MediaAiAnalysis analysis = MediaAiAnalysis.builder()
                .id(UUID.randomUUID())
                .mediaFile(imageMediaFile)
                .project(project)
                .status(AiAnalysisStatus.COMPLETED)
                .aiProvider("YOLO11")
                .analysisSummary("Latest analysis")
                .createdAt(LocalDateTime.now())
                .build();

        when(projectAccessService.validateProjectReadAccess("PRJ-YOLO11")).thenReturn(project);
        when(mediaFileRepository.findByIdAndProject_ProjectCode(mediaId, "PRJ-YOLO11"))
                .thenReturn(Optional.of(imageMediaFile));
        when(supabaseStorageService.generateSignedUrl(imageMediaFile.getStoragePath()))
                .thenReturn("https://storage.supabase.co/signed-url");
        when(mediaAiAnalysisRepository.findByMediaFile_IdOrderByCreatedAtDesc(mediaId))
                .thenReturn(List.of(analysis));

        MediaAiAnalysisResponse response = mlAnalysisService.getLatestAnalysis("PRJ-YOLO11", mediaId);

        assertNotNull(response);
        assertEquals("Latest analysis", response.getAnalysisSummary());
    }

    @Test
    @DisplayName("getLatestAnalysis throws ResourceNotFoundException when no analyses exist")
    void getLatestAnalysis_NotFound_ThrowsResourceNotFound() {
        UUID mediaId = imageMediaFile.getId();

        when(projectAccessService.validateProjectReadAccess("PRJ-YOLO11")).thenReturn(project);
        when(mediaFileRepository.findByIdAndProject_ProjectCode(mediaId, "PRJ-YOLO11"))
                .thenReturn(Optional.of(imageMediaFile));
        when(mediaAiAnalysisRepository.findByMediaFile_IdOrderByCreatedAtDesc(mediaId))
                .thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () ->
                mlAnalysisService.getLatestAnalysis("PRJ-YOLO11", mediaId)
        );
    }

    @Test
    @DisplayName("getAnalysisById returns specific analysis when authorized")
    void getAnalysisById_Success() {
        UUID analysisId = UUID.randomUUID();
        MediaAiAnalysis analysis = MediaAiAnalysis.builder()
                .id(analysisId)
                .mediaFile(imageMediaFile)
                .project(project)
                .status(AiAnalysisStatus.COMPLETED)
                .aiProvider("YOLO11")
                .analysisSummary("Specific analysis by ID")
                .createdAt(LocalDateTime.now())
                .build();

        when(projectAccessService.validateProjectReadAccess("PRJ-YOLO11")).thenReturn(project);
        when(mediaAiAnalysisRepository.findByIdAndProject_ProjectCode(analysisId, "PRJ-YOLO11"))
                .thenReturn(Optional.of(analysis));
        when(supabaseStorageService.generateSignedUrl(imageMediaFile.getStoragePath()))
                .thenReturn("https://storage.supabase.co/signed-url");

        MediaAiAnalysisResponse response = mlAnalysisService.getAnalysisById("PRJ-YOLO11", analysisId);

        assertNotNull(response);
        assertEquals(analysisId, response.getId());
        assertEquals("Specific analysis by ID", response.getAnalysisSummary());
    }
}
