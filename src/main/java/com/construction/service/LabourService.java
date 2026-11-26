package com.construction.service;

import com.construction.dto.labour.CreateLabourRecordRequest;
import com.construction.dto.labour.LabourRecordResponse;
import com.construction.entity.LabourRecord;
import com.construction.entity.Profile;
import com.construction.entity.Project;
import com.construction.entity.ProjectPhase;
import com.construction.entity.enums.AuditAction;
import com.construction.exception.ResourceNotFoundException;
import com.construction.repository.LabourRecordRepository;
import com.construction.repository.ProfileRepository;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabourService {

    private final LabourRecordRepository labourRecordRepository;
    private final ProjectPhaseRepository projectPhaseRepository;
    private final ProfileRepository profileRepository;
    private final ProjectAccessService projectAccessService;
    private final AuditLogService auditLogService;

    @Transactional
    public LabourRecordResponse createLabourRecord(String projectCode, CreateLabourRecordRequest request) {
        Project project = projectAccessService.validateProjectWriteAccess(projectCode);
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Profile recordedBy = profileRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found: " + currentUserId));

        ProjectPhase phase = null;
        if (request.getPhaseId() != null) {
            phase = projectPhaseRepository.findByIdAndProject_ProjectCode(request.getPhaseId(), projectCode)
                    .orElse(null);
        }

        BigDecimal totalCost = request.getTotalCost();
        if (totalCost == null && request.getHourlyRate() != null && request.getHoursWorked() != null) {
            BigDecimal headCount = BigDecimal.valueOf(Math.max(1, request.getHeadCount()));
            totalCost = request.getHourlyRate().multiply(request.getHoursWorked()).multiply(headCount);
        }

        LabourRecord record = LabourRecord.builder()
                .project(project)
                .phase(phase)
                .workDate(request.getWorkDate() != null ? request.getWorkDate() : LocalDate.now())
                .tradeType(request.getTradeType())
                .headCount(request.getHeadCount())
                .hoursWorked(request.getHoursWorked())
                .hourlyRate(request.getHourlyRate())
                .totalCost(totalCost)
                .remarks(request.getRemarks())
                .recordedBy(recordedBy)
                .build();

        record = labourRecordRepository.save(record);

        auditLogService.record("labour_records", record.getId().toString(), AuditAction.CREATE, currentUserId,
                "Logged labour: " + record.getHeadCount() + " " + record.getTradeType() + " for " + record.getHoursWorked() + " hrs on project " + projectCode);

        return mapToResponse(record);
    }

    @Transactional(readOnly = true)
    public Page<LabourRecordResponse> getLabourRecords(String projectCode, Pageable pageable) {
        projectAccessService.validateProjectReadAccess(projectCode);
        return labourRecordRepository.findByProject_ProjectCodeOrderByWorkDateDesc(projectCode, pageable)
                .map(this::mapToResponse);
    }

    private LabourRecordResponse mapToResponse(LabourRecord r) {
        return LabourRecordResponse.builder()
                .id(r.getId())
                .phaseId(r.getPhase() != null ? r.getPhase().getId() : null)
                .phaseName(r.getPhase() != null ? r.getPhase().getName() : null)
                .workDate(r.getWorkDate())
                .tradeType(r.getTradeType())
                .headCount(r.getHeadCount())
                .hoursWorked(r.getHoursWorked())
                .hourlyRate(r.getHourlyRate())
                .totalCost(r.getTotalCost())
                .remarks(r.getRemarks())
                .recordedById(r.getRecordedBy().getId())
                .recordedByName(r.getRecordedBy().getFullName() != null ? r.getRecordedBy().getFullName() : r.getRecordedBy().getEmail())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
