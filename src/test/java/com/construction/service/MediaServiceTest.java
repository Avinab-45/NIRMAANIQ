package com.construction.service;

import com.construction.dto.media.MediaResponse;
import com.construction.entity.MediaFile;
import com.construction.entity.Profile;
import com.construction.entity.Project;
import com.construction.entity.enums.AccountStatus;
import com.construction.entity.enums.MediaCategory;
import com.construction.exception.ApiException;
import com.construction.exception.BadRequestException;
import com.construction.repository.MediaFileRepository;
import com.construction.repository.ProfileRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaFileRepository mediaFileRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private com.construction.repository.ProgressUpdateRepository progressUpdateRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private SupabaseStorageService supabaseStorageService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private MediaService mediaService;

    private UUID userId;
    private Project project;
    private Profile uploaderProfile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        uploaderProfile = Profile.builder()
                .id(userId)
                .email("contractor@test.com")
                .fullName("Test Contractor")
                .status(AccountStatus.ACTIVE)
                .build();

        project = Project.builder()
                .id(UUID.randomUUID())
                .projectCode("PRJ-MEDIA1")
                .name("Media Test Project")
                .createdBy(uploaderProfile)
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
    @DisplayName("Successful media upload stores file in private bucket and saves record")
    void uploadMedia_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "site_progress.jpg", "image/jpeg", "image bytes".getBytes()
        );

        when(projectAccessService.validateProjectWriteAccess("PRJ-MEDIA1")).thenReturn(project);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(uploaderProfile));
        when(supabaseStorageService.uploadFile(eq("PRJ-MEDIA1"), eq(file))).thenReturn("PRJ-MEDIA1/uuid-site_progress.jpg");
        when(supabaseStorageService.generateSignedUrl("PRJ-MEDIA1/uuid-site_progress.jpg"))
                .thenReturn("https://project.supabase.co/storage/v1/object/sign/construction-media/PRJ-MEDIA1/uuid-site_progress.jpg?token=abc");

        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(i -> {
            MediaFile mf = i.getArgument(0);
            mf.setId(UUID.randomUUID());
            mf.setCreatedAt(LocalDateTime.now());
            return mf;
        });

        MediaResponse response = mediaService.uploadMedia("PRJ-MEDIA1", file, MediaCategory.SITE_PHOTO, "Foundation progress");

        assertNotNull(response);
        assertEquals("site_progress.jpg", response.getFileName());
        assertEquals("PRJ-MEDIA1/uuid-site_progress.jpg", response.getStoragePath());
        assertTrue(response.getSignedUrl().contains("token=abc"));
        verify(mediaFileRepository).save(any(MediaFile.class));
    }

    @Test
    @DisplayName("Supabase Storage upload failure throws ApiException and DOES NOT save database record")
    void uploadMedia_StorageUploadFailure_DoesNotSaveRecord() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "site_photo.png", "image/png", "png data".getBytes()
        );

        when(projectAccessService.validateProjectWriteAccess("PRJ-MEDIA1")).thenReturn(project);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(uploaderProfile));
        when(supabaseStorageService.uploadFile(eq("PRJ-MEDIA1"), eq(file)))
                .thenThrow(new ApiException("Supabase Storage upload failed: 502", HttpStatus.BAD_GATEWAY));

        ApiException ex = assertThrows(ApiException.class,
                () -> mediaService.uploadMedia("PRJ-MEDIA1", file, MediaCategory.SITE_PHOTO, "Caption"));
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
        verify(mediaFileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Supabase Storage signed URL failure throws ApiException, DOES NOT save database record, and cleans up uploaded file")
    void uploadMedia_SignedUrlFailure_DoesNotSaveRecord() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "site_doc.pdf", "application/pdf", "pdf data".getBytes()
        );

        when(projectAccessService.validateProjectWriteAccess("PRJ-MEDIA1")).thenReturn(project);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(uploaderProfile));
        when(supabaseStorageService.uploadFile(eq("PRJ-MEDIA1"), eq(file))).thenReturn("PRJ-MEDIA1/uuid-site_doc.pdf");
        when(supabaseStorageService.generateSignedUrl("PRJ-MEDIA1/uuid-site_doc.pdf"))
                .thenThrow(new ApiException("Failed to generate signed URL", HttpStatus.INTERNAL_SERVER_ERROR));

        ApiException ex = assertThrows(ApiException.class,
                () -> mediaService.uploadMedia("PRJ-MEDIA1", file, MediaCategory.DOCUMENT, "Doc caption"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        verify(mediaFileRepository, never()).save(any());
        // Verify storage cleanup behavior
        verify(supabaseStorageService).deleteFile("PRJ-MEDIA1/uuid-site_doc.pdf");
    }

    @Test
    @DisplayName("Database save failure after storage upload cleans up uploaded file from storage")
    void uploadMedia_DatabaseSaveFailure_CleansUpUploadedFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "site_photo.jpg", "image/jpeg", "image bytes".getBytes()
        );

        when(projectAccessService.validateProjectWriteAccess("PRJ-MEDIA1")).thenReturn(project);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(uploaderProfile));
        when(supabaseStorageService.uploadFile(eq("PRJ-MEDIA1"), eq(file))).thenReturn("PRJ-MEDIA1/uuid-site_photo.jpg");
        when(supabaseStorageService.generateSignedUrl("PRJ-MEDIA1/uuid-site_photo.jpg"))
                .thenReturn("https://project.supabase.co/storage/v1/object/sign/construction-media/PRJ-MEDIA1/uuid-site_photo.jpg?token=abc");
        when(mediaFileRepository.save(any(MediaFile.class)))
                .thenThrow(new RuntimeException("Simulated database constraint or connection failure"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mediaService.uploadMedia("PRJ-MEDIA1", file, MediaCategory.SITE_PHOTO, "Caption"));
        assertTrue(ex.getMessage().contains("database constraint"));
        // Verify storage cleanup behavior on downstream DB failure
        verify(supabaseStorageService).deleteFile("PRJ-MEDIA1/uuid-site_photo.jpg");
    }

    @Test
    @DisplayName("Empty file upload throws BadRequestException")
    void uploadMedia_EmptyFile_ThrowsBadRequest() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        when(projectAccessService.validateProjectWriteAccess("PRJ-MEDIA1")).thenReturn(project);

        assertThrows(BadRequestException.class,
                () -> mediaService.uploadMedia("PRJ-MEDIA1", emptyFile, MediaCategory.SITE_PHOTO, "caption"));
        verifyNoInteractions(supabaseStorageService);
        verify(mediaFileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Unsupported MIME type throws BadRequestException")
    void uploadMedia_UnsupportedMimeType_ThrowsBadRequest() {
        MockMultipartFile exeFile = new MockMultipartFile("file", "script.exe", "application/octet-stream", "dummy".getBytes());

        when(projectAccessService.validateProjectWriteAccess("PRJ-MEDIA1")).thenReturn(project);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> mediaService.uploadMedia("PRJ-MEDIA1", exeFile, MediaCategory.OTHER, "caption"));
        assertTrue(ex.getMessage().contains("Unsupported file type"));
        verifyNoInteractions(supabaseStorageService);
    }

    @Test
    @DisplayName("Upload site photograph successfully linked to daily progress report")
    void uploadMedia_WithValidProgressUpdate_Success() {
        UUID progressId = UUID.randomUUID();
        com.construction.entity.ProgressUpdate progressUpdate = com.construction.entity.ProgressUpdate.builder()
                .id(progressId)
                .project(project)
                .percentageCompleted(new java.math.BigDecimal("40.0"))
                .title("Brickwork Day 1")
                .build();

        MockMultipartFile file = new MockMultipartFile(
                "file", "site_progress.jpg", "image/jpeg", "image bytes".getBytes()
        );

        when(projectAccessService.validateProjectWriteAccess("PRJ-MEDIA1")).thenReturn(project);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(uploaderProfile));
        when(progressUpdateRepository.findById(progressId)).thenReturn(Optional.of(progressUpdate));
        when(supabaseStorageService.uploadFile(eq("PRJ-MEDIA1"), eq(file))).thenReturn("PRJ-MEDIA1/uuid-site_progress.jpg");
        when(supabaseStorageService.generateSignedUrl("PRJ-MEDIA1/uuid-site_progress.jpg"))
                .thenReturn("https://project.supabase.co/storage/v1/object/sign/construction-media/PRJ-MEDIA1/uuid-site_progress.jpg?token=abc");

        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(i -> {
            MediaFile mf = i.getArgument(0);
            mf.setId(UUID.randomUUID());
            mf.setCreatedAt(LocalDateTime.now());
            return mf;
        });

        MediaResponse response = mediaService.uploadMedia("PRJ-MEDIA1", file, MediaCategory.SITE_PHOTO, "Foundation progress", progressId);

        assertNotNull(response);
        assertEquals(progressId, response.getProgressUpdateId());
        assertEquals("site_progress.jpg", response.getFileName());
        verify(mediaFileRepository).save(any(MediaFile.class));
    }

    @Test
    @DisplayName("Upload site photograph linked to wrong project throws BadRequestException")
    void uploadMedia_ProgressUpdateWrongProject_ThrowsBadRequest() {
        UUID progressId = UUID.randomUUID();
        Project otherProject = Project.builder()
                .id(UUID.randomUUID())
                .projectCode("PRJ-OTHER99")
                .name("Other Project")
                .build();

        com.construction.entity.ProgressUpdate otherProjectProgress = com.construction.entity.ProgressUpdate.builder()
                .id(progressId)
                .project(otherProject)
                .percentageCompleted(new java.math.BigDecimal("50.0"))
                .title("Other Project Progress")
                .build();

        MockMultipartFile file = new MockMultipartFile(
                "file", "site_progress.jpg", "image/jpeg", "image bytes".getBytes()
        );

        when(projectAccessService.validateProjectWriteAccess("PRJ-MEDIA1")).thenReturn(project);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(uploaderProfile));
        when(progressUpdateRepository.findById(progressId)).thenReturn(Optional.of(otherProjectProgress));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> mediaService.uploadMedia("PRJ-MEDIA1", file, MediaCategory.SITE_PHOTO, "Progress photo", progressId));

        assertTrue(ex.getMessage().contains("does not belong to project"));
        verifyNoInteractions(supabaseStorageService);
        verify(mediaFileRepository, never()).save(any());
    }
}
