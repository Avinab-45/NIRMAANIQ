package com.construction.service;

import com.construction.dto.phase.CreatePhaseRequest;
import com.construction.dto.phase.PhaseResponse;
import com.construction.dto.phase.UpdatePhaseRequest;
import com.construction.entity.Project;
import com.construction.entity.ProjectPhase;
import com.construction.entity.enums.AuditAction;
import com.construction.entity.enums.PhaseStatus;
import com.construction.exception.ResourceNotFoundException;
import com.construction.repository.ProjectPhaseRepository;
import com.construction.security.ProjectAccessService;
import com.construction.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectPhaseService {

    private final ProjectPhaseRepository projectPhaseRepository;
    private final ProjectAccessService projectAccessService;
    private final AuditLogService auditLogService;

    @Transactional
    public PhaseResponse createPhase(String projectCode, CreatePhaseRequest request) {
        Project project = projectAccessService.validateProjectOwner(projectCode);
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        ProjectPhase phase = ProjectPhase.builder()
                .project(project)
                .name(request.getName().trim())
                .description(request.getDescription())
                .sequenceOrder(request.getSequenceOrder())
                .status(PhaseStatus.NOT_STARTED)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .targetCompletionDate(request.getTargetCompletionDate())
                .budget(request.getBudget())
                .build();

        phase = projectPhaseRepository.save(phase);

        auditLogService.record("project_phases", phase.getId().toString(), AuditAction.CREATE, currentUserId,
                "Created phase: " + phase.getName() + " for project " + projectCode);

        return mapToResponse(phase);
    }

    @Transactional(readOnly = true)
    public List<PhaseResponse> getPhases(String projectCode) {
        projectAccessService.validateProjectReadAccess(projectCode);
        return projectPhaseRepository.findByProject_ProjectCodeOrderBySequenceOrderAsc(projectCode)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public PhaseResponse updatePhase(String projectCode, UUID phaseId, UpdatePhaseRequest request) {
        projectAccessService.validateProjectWriteAccess(projectCode);
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        ProjectPhase phase = projectPhaseRepository.findByIdAndProject_ProjectCode(phaseId, projectCode)
                .orElseThrow(() -> new ResourceNotFoundException("Phase not found: " + phaseId + " in project " + projectCode));

        if (request.getName() != null && !request.getName().isBlank()) {
            phase.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            phase.setDescription(request.getDescription());
        }
        if (request.getSequenceOrder() != null) {
            phase.setSequenceOrder(request.getSequenceOrder());
        }
        if (request.getStatus() != null) {
            phase.setStatus(request.getStatus());
        }
        if (request.getStartDate() != null) {
            phase.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            phase.setEndDate(request.getEndDate());
        }
        if (request.getTargetCompletionDate() != null) {
            phase.setTargetCompletionDate(request.getTargetCompletionDate());
        }
        if (request.getBudget() != null) {
            phase.setBudget(request.getBudget());
        }

        phase = projectPhaseRepository.save(phase);

        auditLogService.record("project_phases", phase.getId().toString(), AuditAction.UPDATE, currentUserId,
                "Updated phase: " + phase.getName() + " status=" + phase.getStatus());

        return mapToResponse(phase);
    }

    private PhaseResponse mapToResponse(ProjectPhase phase) {
        return PhaseResponse.builder()
                .id(phase.getId())
                .name(phase.getName())
                .description(phase.getDescription())
                .sequenceOrder(phase.getSequenceOrder())
                .status(phase.getStatus())
                .startDate(phase.getStartDate())
                .endDate(phase.getEndDate())
                .targetCompletionDate(phase.getTargetCompletionDate())
                .budget(phase.getBudget())
                .createdAt(phase.getCreatedAt())
                .updatedAt(phase.getUpdatedAt())
                .build();
    }
}
