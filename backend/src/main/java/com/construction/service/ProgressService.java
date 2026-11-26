package com.construction.service;

import com.construction.dto.progress.CreateProgressRequest;
import com.construction.dto.progress.ProgressResponse;
import com.construction.entity.Profile;
import com.construction.entity.Project;
import com.construction.entity.ProjectPhase;
import com.construction.entity.ProgressUpdate;
import com.construction.entity.enums.AuditAction;
import com.construction.exception.BadRequestException;
import com.construction.exception.ResourceNotFoundException;
import com.construction.repository.ProfileRepository;
import com.construction.repository.ProgressUpdateRepository;
import com.construction.repository.ProjectPhaseRepository;
import com.construction.security.ProjectAccessService;
import com.construction.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ProgressUpdateRepository progressUpdateRepository;
    private final ProjectPhaseRepository projectPhaseRepository;
    private final ProfileRepository profileRepository;
    private final ProjectAccessService projectAccessService;
    private final AuditLogService auditLogService;

    public static final Set<String> ALLOWED_AREA_UNITS = Set.of("SQFT", "SQM", "CFT", "RFT", "UNITS");

    @Transactional
    public ProgressResponse createProgressUpdate(String projectCode, CreateProgressRequest request) {
        Project project = projectAccessService.validateProjectWriteAccess(projectCode);
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Profile submittedBy = profileRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found: " + currentUserId));

        // Validate percentage between 0 and 100
        if (request.getPercentageCompleted() == null) {
            throw new BadRequestException("Percentage completed is required.");
        }
        if (request.getPercentageCompleted().compareTo(BigDecimal.ZERO) < 0
                || request.getPercentageCompleted().compareTo(new BigDecimal("100.0")) > 0) {
            throw new BadRequestException("Percentage completed must be between 0.0 and 100.0.");
        }

        // Validate non-negative area completed
        if (request.getAreaCompleted() != null && request.getAreaCompleted().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Area completed cannot be negative.");
        }

        // Validate measurement unit
        String normalizedUnit = null;
        if (request.getAreaUnit() != null && !request.getAreaUnit().isBlank()) {
            normalizedUnit = request.getAreaUnit().trim().toUpperCase();
            if (!ALLOWED_AREA_UNITS.contains(normalizedUnit)) {
                throw new BadRequestException("Invalid measurement unit: '" + request.getAreaUnit()
                        + "'. Allowed units: " + String.join(", ", ALLOWED_AREA_UNITS));
            }
        }

        // Validate phase belongs to the project
        ProjectPhase phase = null;
        if (request.getPhaseId() != null) {
            phase = projectPhaseRepository.findByIdAndProject_ProjectCode(request.getPhaseId(), projectCode)
                    .orElseThrow(() -> new BadRequestException(
                            "Project phase not found or does not belong to project " + projectCode + ": " + request.getPhaseId()));
        }

        // Preserve historical daily records without overwriting previous updates
        ProgressUpdate progress = ProgressUpdate.builder()
                .project(project)
                .phase(phase)
                .submittedBy(submittedBy)
                .workCategory(request.getWorkCategory() != null ? request.getWorkCategory().trim() : null)
                .areaCompleted(request.getAreaCompleted())
                .areaUnit(normalizedUnit)
                .percentageCompleted(request.getPercentageCompleted())
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .workCompletedToday(request.getWorkCompletedToday())
                .blockersDelays(request.getBlockersDelays())
                .hasBlockers(request.isHasBlockers())
                .weatherNotes(request.getWeatherNotes())
                .updateDate(request.getUpdateDate() != null ? request.getUpdateDate() : LocalDate.now())
                .build();

        progress = progressUpdateRepository.save(progress);

        auditLogService.record("progress_updates", progress.getId().toString(), AuditAction.CREATE, currentUserId,
                "Progress recorded for " + projectCode + ": " + progress.getPercentageCompleted() + "% ("
                        + (progress.getAreaCompleted() != null ? progress.getAreaCompleted() + " " + progress.getAreaUnit() : "N/A")
                        + ") - " + progress.getTitle());

        return mapToResponse(progress);
    }

    @Transactional(readOnly = true)
    public Page<ProgressResponse> getProgressUpdates(String projectCode, Pageable pageable) {
        projectAccessService.validateProjectReadAccess(projectCode);
        return progressUpdateRepository.findByProject_ProjectCodeOrderByCreatedAtDesc(projectCode, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ProgressResponse> getProgressUpdatesByDate(String projectCode, LocalDate date) {
        projectAccessService.validateProjectReadAccess(projectCode);
        return progressUpdateRepository.findByProject_ProjectCodeAndUpdateDateOrderByCreatedAtDesc(projectCode, date)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProgressResponse> getProgressUpdatesByDateRange(String projectCode, LocalDate startDate, LocalDate endDate) {
        projectAccessService.validateProjectReadAccess(projectCode);
        return progressUpdateRepository.findByProject_ProjectCodeAndUpdateDateBetweenOrderByUpdateDateDescCreatedAtDesc(projectCode, startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgressResponse getLatestProgressUpdate(String projectCode) {
        projectAccessService.validateProjectReadAccess(projectCode);
        ProgressUpdate latest = progressUpdateRepository.findFirstByProject_ProjectCodeOrderByCreatedAtDesc(projectCode)
                .orElseThrow(() -> new ResourceNotFoundException("No progress updates found for project: " + projectCode));
        return mapToResponse(latest);
    }

    private ProgressResponse mapToResponse(ProgressUpdate p) {
        return ProgressResponse.builder()
                .id(p.getId())
                .phaseId(p.getPhase() != null ? p.getPhase().getId() : null)
                .phaseName(p.getPhase() != null ? p.getPhase().getName() : null)
                .submittedById(p.getSubmittedBy().getId())
                .submittedByName(p.getSubmittedBy().getFullName() != null ? p.getSubmittedBy().getFullName() : p.getSubmittedBy().getEmail())
                .submittedByEmail(p.getSubmittedBy().getEmail())
                .workCategory(p.getWorkCategory())
                .areaCompleted(p.getAreaCompleted())
                .areaUnit(p.getAreaUnit())
                .percentageCompleted(p.getPercentageCompleted())
                .title(p.getTitle())
                .description(p.getDescription())
                .workCompletedToday(p.getWorkCompletedToday())
                .blockersDelays(p.getBlockersDelays())
                .hasBlockers(p.isHasBlockers())
                .weatherNotes(p.getWeatherNotes())
                .updateDate(p.getUpdateDate())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
