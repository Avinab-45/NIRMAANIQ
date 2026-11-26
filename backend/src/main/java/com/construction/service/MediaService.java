package com.construction.service;

import com.construction.dto.media.MediaResponse;
import com.construction.entity.MediaFile;
import com.construction.entity.Profile;
import com.construction.entity.Project;
import com.construction.entity.ProgressUpdate;
import com.construction.entity.enums.AuditAction;
import com.construction.entity.enums.MediaCategory;
import com.construction.exception.BadRequestException;
import com.construction.exception.ForbiddenException;
import com.construction.exception.ResourceNotFoundException;
import com.construction.repository.MediaFileRepository;
import com.construction.repository.ProfileRepository;
import com.construction.repository.ProgressUpdateRepository;
import com.construction.security.ProjectAccessService;
import com.construction.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaFileRepository mediaFileRepository;
    private final ProfileRepository profileRepository;
    private final ProgressUpdateRepository progressUpdateRepository;
    private final ProjectAccessService projectAccessService;
    private final SupabaseStorageService supabaseStorageService;
    private final AuditLogService auditLogService;

    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024; // 25 MB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif",
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    @Transactional
    public MediaResponse uploadMedia(String projectCode, MultipartFile file, MediaCategory category, String caption) {
        return uploadMedia(projectCode, file, category, caption, null);
    }

    @Transactional
    public MediaResponse uploadMedia(String projectCode, MultipartFile file, MediaCategory category, String caption, UUID progressUpdateId) {
        Project project = projectAccessService.validateProjectWriteAccess(projectCode);
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds 25MB limit.");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            throw new BadRequestException("Unsupported file type: " + mimeType + ". Supported: JPG, PNG, WEBP, PDF, DOCX, XLSX.");
        }

        Profile uploader = profileRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found: " + currentUserId));

        // Validate and link corresponding daily progress report if provided
        ProgressUpdate progressUpdate = null;
        if (progressUpdateId != null) {
            progressUpdate = progressUpdateRepository.findById(progressUpdateId)
                    .orElseThrow(() -> new ResourceNotFoundException("Progress update not found: " + progressUpdateId));

            // Verify progress report belongs to the same project
            if (!progressUpdate.getProject().getId().equals(project.getId())) {
                log.warn("Security violation: Attempted to link progress update {} to wrong project {}", progressUpdateId, projectCode);
                throw new BadRequestException("Progress update " + progressUpdateId + " does not belong to project " + projectCode);
            }
        }

        // Upload to Supabase Storage - throws ApiException if storage rejects upload
        String storagePath = supabaseStorageService.uploadFile(projectCode, file);

        String signedUrl;
        MediaFile mediaFile;
        try {
            // Generate genuine signed URL for private bucket - throws ApiException if generation fails
            signedUrl = supabaseStorageService.generateSignedUrl(storagePath);

            // Persist media record ONLY after successful upload and signed URL generation
            mediaFile = MediaFile.builder()
                    .project(project)
                    .uploadedBy(uploader)
                    .progressUpdate(progressUpdate)
                    .fileName(file.getOriginalFilename())
                    .storagePath(storagePath)
                    .fileSize(file.getSize())
                    .mimeType(mimeType)
                    .mediaCategory(category != null ? category : MediaCategory.SITE_PHOTO)
                    .caption(caption)
                    .build();

            mediaFile = mediaFileRepository.save(mediaFile);
        } catch (Exception ex) {
            log.error("Failed downstream processing after storage upload for {}. Cleaning up uploaded storage file: {}", storagePath, ex.getMessage());
            try {
                supabaseStorageService.deleteFile(storagePath);
            } catch (Exception deleteEx) {
                log.error("Failed to cleanup storage file {} after failure: {}", storagePath, deleteEx.getMessage());
            }
            throw ex;
        }

        auditLogService.record("media_files", mediaFile.getId().toString(), AuditAction.CREATE, currentUserId,
                "Uploaded media: " + mediaFile.getFileName() + " (" + mediaFile.getMediaCategory() + ") to project " + projectCode
                        + (progressUpdate != null ? " linked to progress " + progressUpdate.getId() : ""));

        return mapToResponse(mediaFile, signedUrl);
    }

    @Transactional(readOnly = true)
    public List<MediaResponse> getMediaFiles(String projectCode, MediaCategory category) {
        projectAccessService.validateProjectReadAccess(projectCode);

        List<MediaFile> files;
        if (category != null) {
            files = mediaFileRepository.findByProject_ProjectCodeAndMediaCategoryOrderByCreatedAtDesc(projectCode, category);
        } else {
            files = mediaFileRepository.findByProject_ProjectCodeOrderByCreatedAtDesc(projectCode);
        }

        return files.stream()
                .map(file -> {
                    String signedUrl = supabaseStorageService.generateSignedUrl(file.getStoragePath());
                    return mapToResponse(file, signedUrl);
                })
                .toList();
    }

    @Transactional
    public void deleteMediaFile(String projectCode, UUID mediaId) {
        Project project = projectAccessService.validateProjectWriteAccess(projectCode);
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        MediaFile mediaFile = mediaFileRepository.findByIdAndProject_ProjectCode(mediaId, projectCode)
                .orElseThrow(() -> new ResourceNotFoundException("Media file not found: " + mediaId));

        // Only builder owner or the original uploader can delete
        boolean isOwner = project.getCreatedBy().getId().equals(currentUserId);
        boolean isUploader = mediaFile.getUploadedBy().getId().equals(currentUserId);
        if (!isOwner && !isUploader) {
            throw new ForbiddenException("You do not have permission to delete this media file.");
        }

        // Delete from Supabase Storage (fails transaction if delete fails)
        supabaseStorageService.deleteFile(mediaFile.getStoragePath());

        // Delete record from database
        mediaFileRepository.delete(mediaFile);

        auditLogService.record("media_files", mediaId.toString(), AuditAction.DELETE, currentUserId,
                "Deleted media file: " + mediaFile.getFileName() + " from project " + projectCode);
    }

    private MediaResponse mapToResponse(MediaFile mf, String signedUrl) {
        return MediaResponse.builder()
                .id(mf.getId())
                .progressUpdateId(mf.getProgressUpdate() != null ? mf.getProgressUpdate().getId() : null)
                .fileName(mf.getFileName())
                .storagePath(mf.getStoragePath())
                .signedUrl(signedUrl)
                .fileSize(mf.getFileSize())
                .mimeType(mf.getMimeType())
                .mediaCategory(mf.getMediaCategory())
                .caption(mf.getCaption())
                .uploadedById(mf.getUploadedBy().getId())
                .uploadedByName(mf.getUploadedBy().getFullName() != null ? mf.getUploadedBy().getFullName() : mf.getUploadedBy().getEmail())
                .createdAt(mf.getCreatedAt())
                .build();
    }
}
