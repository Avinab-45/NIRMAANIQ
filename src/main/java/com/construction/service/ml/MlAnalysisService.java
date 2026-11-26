package com.construction.service.ml;

import com.construction.config.MlProperties;
import com.construction.dto.ai.MediaAiAnalysisResponse;
import com.construction.dto.ml.FastApiPredictResponse;
import com.construction.entity.MediaAiAnalysis;
import com.construction.entity.MediaFile;
import com.construction.entity.Profile;
import com.construction.entity.Project;
import com.construction.entity.enums.AiAnalysisStatus;
import com.construction.entity.enums.AuditAction;
import com.construction.exception.BadRequestException;
import com.construction.exception.ResourceNotFoundException;
import com.construction.repository.MediaAiAnalysisRepository;
import com.construction.repository.MediaFileRepository;
import com.construction.repository.ProfileRepository;
import com.construction.security.ProjectAccessService;
import com.construction.security.SecurityUtil;
import com.construction.service.AuditLogService;
import com.construction.service.SupabaseStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service orchestrating YOLO11 visual analysis for construction site photographs.
 * Manages the persistent analysis lifecycle in media_ai_analyses, handles idempotency,
 * and maintains strict separation from manual contractor progress metrics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MlAnalysisService {

    private final MediaFileRepository mediaFileRepository;
    private final MediaAiAnalysisRepository mediaAiAnalysisRepository;
    private final ProfileRepository profileRepository;
    private final ProjectAccessService projectAccessService;
    private final SupabaseStorageService supabaseStorageService;
    private final MlServiceClient mlServiceClient;
    private final AuditLogService auditLogService;
    private final MlProperties mlProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> SUPPORTED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    /**
     * Executes YOLO11 ML analysis on a project site photograph.
     * Enforces idempotency unless explicitly requested to reanalyze.
     *
     * @param projectCode Unique project identifier
     * @param mediaId     Photograph UUID
     * @param reanalyze   If true, forces re-running model inference even if already completed
     * @return MediaAiAnalysisResponse persistent analysis record with detections
     */
    @Transactional
    public MediaAiAnalysisResponse analyzeMediaFile(String projectCode, UUID mediaId, boolean reanalyze) {
        Project project = projectAccessService.validateProjectWriteAccess(projectCode);
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Profile analyzedBy = profileRepository.findById(currentUserId).orElse(null);

        MediaFile mediaFile = mediaFileRepository.findByIdAndProject_ProjectCode(mediaId, projectCode)
                .orElseThrow(() -> new ResourceNotFoundException("Media file " + mediaId + " not found in project " + projectCode));

        String mimeType = mediaFile.getMimeType();
        if (mimeType == null || !SUPPORTED_IMAGE_MIME_TYPES.contains(mimeType.toLowerCase())) {
            throw new BadRequestException("YOLO11 photograph analysis is only supported for image files (JPEG, PNG, WEBP). Found: " + mimeType);
        }

        // Idempotency check: if not forced to reanalyze, reuse existing COMPLETED or in-flight analysis
        List<MediaAiAnalysis> existingAnalyses = mediaAiAnalysisRepository.findByMediaFile_IdOrderByCreatedAtDesc(mediaId);
        if (!existingAnalyses.isEmpty() && !reanalyze) {
            MediaAiAnalysis latest = existingAnalyses.get(0);
            if (latest.getStatus() == AiAnalysisStatus.COMPLETED) {
                log.info("Idempotent hit: returning existing COMPLETED analysis {} for media {}", latest.getId(), mediaId);
                String signedUrl = supabaseStorageService.generateSignedUrl(mediaFile.getStoragePath());
                return mapToResponse(latest, signedUrl);
            }
            if (latest.getStatus() == AiAnalysisStatus.PROCESSING) {
                // If created within the last 2 minutes, avoid duplicate concurrent inference
                if (latest.getCreatedAt() != null && latest.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(2))) {
                    log.info("In-flight analysis in progress for media {}. Returning existing record {}.", mediaId, latest.getId());
                    String signedUrl = supabaseStorageService.generateSignedUrl(mediaFile.getStoragePath());
                    return mapToResponse(latest, signedUrl);
                }
            }
        }

        // Step 1: Create initial PENDING record
        MediaAiAnalysis analysis = MediaAiAnalysis.builder()
                .mediaFile(mediaFile)
                .project(project)
                .analyzedBy(analyzedBy)
                .status(AiAnalysisStatus.PENDING)
                .aiProvider("YOLO11_FASTAPI")
                .createdAt(LocalDateTime.now())
                .build();
        analysis = mediaAiAnalysisRepository.save(analysis);

        // Step 2: Transition to PROCESSING
        analysis.setStatus(AiAnalysisStatus.PROCESSING);
        analysis = mediaAiAnalysisRepository.save(analysis);

        // Step 3: Check if ML service is enabled
        if (!mlProperties.isEnabled()) {
            log.warn("YOLO11 ML analysis skipped: ML service is disabled (ML_SERVICE_ENABLED=false)");
            analysis.setStatus(AiAnalysisStatus.FAILED);
            analysis.setErrorMessage("ML inference service is disabled (ML_SERVICE_ENABLED=false)");
            analysis.setCompletedAt(LocalDateTime.now());
            analysis = mediaAiAnalysisRepository.save(analysis);

            String signedUrl = supabaseStorageService.generateSignedUrl(mediaFile.getStoragePath());
            return mapToResponse(analysis, signedUrl);
        }

        // Step 4: Securely download photograph bytes from private Supabase Storage
        byte[] imageBytes;
        try {
            imageBytes = supabaseStorageService.downloadFile(mediaFile.getStoragePath());
        } catch (Exception storageEx) {
            log.error("Failed to retrieve image bytes from storage for media {}: {}", mediaId, storageEx.getMessage());
            analysis.setStatus(AiAnalysisStatus.FAILED);
            analysis.setErrorMessage("Failed to retrieve photograph from storage: " + storageEx.getMessage());
            analysis.setCompletedAt(LocalDateTime.now());
            analysis = mediaAiAnalysisRepository.save(analysis);

            String signedUrl = supabaseStorageService.generateSignedUrl(mediaFile.getStoragePath());
            return mapToResponse(analysis, signedUrl);
        }

        // Step 5: Execute YOLO11 inference call to FastAPI
        try {
            FastApiPredictResponse predictResponse = mlServiceClient.predict(
                    imageBytes,
                    mediaFile.getFileName(),
                    mimeType,
                    projectCode,
                    mediaId
            );

            analysis.setStatus(AiAnalysisStatus.COMPLETED);
            analysis.setAiProvider("YOLO11_FASTAPI");
            analysis.setAiModel(predictResponse.getModel() +
                    (predictResponse.getVersion() != null ? ":" + predictResponse.getVersion() : ""));
            analysis.setAnalysisSummary(predictResponse.getSummary());
            analysis.setDetectedWorkCategory(predictResponse.getWorkCategory() != null ?
                    predictResponse.getWorkCategory() : "Automated Visual Inspection");
            analysis.setEstimatedCompletionPercentage(predictResponse.getEstimatedCompletionPercentage());

            // Convert issues and recommendations lists to JSON strings for JSONB columns
            if (predictResponse.getIssues() != null && !predictResponse.getIssues().isEmpty()) {
                analysis.setDetectedIssues(objectMapper.writeValueAsString(predictResponse.getIssues()));
            } else {
                analysis.setDetectedIssues("[]");
            }

            if (predictResponse.getRecommendations() != null && !predictResponse.getRecommendations().isEmpty()) {
                analysis.setRecommendations(objectMapper.writeValueAsString(predictResponse.getRecommendations()));
            } else {
                analysis.setRecommendations("[]");
            }

            analysis.setRawAnalysis(predictResponse.getRawJson() != null ?
                    predictResponse.getRawJson() : "{}");
            analysis.setCompletedAt(LocalDateTime.now());
            analysis.setErrorMessage(null);

            log.info("YOLO11 analysis completed successfully for media {} in project {}", mediaId, projectCode);
        } catch (Exception e) {
            log.error("YOLO11 analysis failed for media {} in project {}: {}", mediaId, projectCode, e.getMessage());
            analysis.setStatus(AiAnalysisStatus.FAILED);
            analysis.setErrorMessage(e.getMessage());
            analysis.setCompletedAt(LocalDateTime.now());
        }

        analysis = mediaAiAnalysisRepository.save(analysis);

        auditLogService.record("media_ai_analyses", analysis.getId().toString(), AuditAction.CREATE, currentUserId,
                "YOLO11 analysis executed on media " + mediaId + " status=" + analysis.getStatus());

        String signedUrl = supabaseStorageService.generateSignedUrl(mediaFile.getStoragePath());
        return mapToResponse(analysis, signedUrl);
    }

    /**
     * Retrieves the latest AI analysis for a specific photograph.
     */
    @Transactional(readOnly = true)
    public MediaAiAnalysisResponse getLatestAnalysis(String projectCode, UUID mediaId) {
        projectAccessService.validateProjectReadAccess(projectCode);

        MediaFile mediaFile = mediaFileRepository.findByIdAndProject_ProjectCode(mediaId, projectCode)
                .orElseThrow(() -> new ResourceNotFoundException("Media file " + mediaId + " not found in project " + projectCode));

        List<MediaAiAnalysis> analyses = mediaAiAnalysisRepository.findByMediaFile_IdOrderByCreatedAtDesc(mediaId);
        if (analyses.isEmpty()) {
            throw new ResourceNotFoundException("No AI analysis history found for media file " + mediaId);
        }

        String signedUrl = supabaseStorageService.generateSignedUrl(mediaFile.getStoragePath());
        return mapToResponse(analyses.get(0), signedUrl);
    }

    /**
     * Retrieves all persistent AI analyses for a specific photograph.
     */
    @Transactional(readOnly = true)
    public List<MediaAiAnalysisResponse> getMediaAnalyses(String projectCode, UUID mediaId) {
        projectAccessService.validateProjectReadAccess(projectCode);

        MediaFile mediaFile = mediaFileRepository.findByIdAndProject_ProjectCode(mediaId, projectCode)
                .orElseThrow(() -> new ResourceNotFoundException("Media file " + mediaId + " not found in project " + projectCode));

        String signedUrl = supabaseStorageService.generateSignedUrl(mediaFile.getStoragePath());

        return mediaAiAnalysisRepository.findByMediaFile_IdOrderByCreatedAtDesc(mediaId)
                .stream()
                .map(a -> mapToResponse(a, signedUrl))
                .toList();
    }

    /**
     * Retrieves all persistent AI analyses across the entire project for Builder and Client reporting.
     */
    @Transactional(readOnly = true)
    public List<MediaAiAnalysisResponse> getProjectAnalyses(String projectCode) {
        projectAccessService.validateProjectReadAccess(projectCode);

        return mediaAiAnalysisRepository.findByProject_ProjectCodeOrderByCreatedAtDesc(projectCode)
                .stream()
                .map(a -> {
                    String signedUrl = supabaseStorageService.generateSignedUrl(a.getMediaFile().getStoragePath());
                    return mapToResponse(a, signedUrl);
                })
                .toList();
    }

    /**
     * Retrieves a single AI analysis by analysis ID within the project.
     */
    @Transactional(readOnly = true)
    public MediaAiAnalysisResponse getAnalysisById(String projectCode, UUID analysisId) {
        projectAccessService.validateProjectReadAccess(projectCode);

        MediaAiAnalysis analysis = mediaAiAnalysisRepository.findByIdAndProject_ProjectCode(analysisId, projectCode)
                .orElseThrow(() -> new ResourceNotFoundException("AI analysis " + analysisId + " not found in project " + projectCode));

        String signedUrl = supabaseStorageService.generateSignedUrl(analysis.getMediaFile().getStoragePath());
        return mapToResponse(analysis, signedUrl);
    }

    private MediaAiAnalysisResponse mapToResponse(MediaAiAnalysis a, String signedUrl) {
        return MediaAiAnalysisResponse.builder()
                .id(a.getId())
                .mediaFileId(a.getMediaFile().getId())
                .mediaFileName(a.getMediaFile().getFileName())
                .mediaSignedUrl(signedUrl)
                .projectId(a.getProject().getId())
                .projectCode(a.getProject().getProjectCode())
                .analyzedById(a.getAnalyzedBy() != null ? a.getAnalyzedBy().getId() : null)
                .analyzedByName(a.getAnalyzedBy() != null ?
                        (a.getAnalyzedBy().getFullName() != null ? a.getAnalyzedBy().getFullName() : a.getAnalyzedBy().getEmail()) : "System")
                .status(a.getStatus())
                .aiProvider(a.getAiProvider())
                .aiModel(a.getAiModel())
                .analysisSummary(a.getAnalysisSummary())
                .detectedWorkCategory(a.getDetectedWorkCategory())
                .estimatedCompletionPercentage(a.getEstimatedCompletionPercentage())
                .detectedIssues(a.getDetectedIssues())
                .recommendations(a.getRecommendations())
                .rawAnalysis(a.getRawAnalysis())
                .errorMessage(a.getErrorMessage())
                .createdAt(a.getCreatedAt())
                .completedAt(a.getCompletedAt())
                .build();
    }
}
