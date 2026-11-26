package com.construction.service;

import com.construction.entity.AuditLog;
import com.construction.entity.Profile;
import com.construction.entity.enums.AuditAction;
import com.construction.repository.AuditLogRepository;
import com.construction.repository.ProfileRepository;
import com.construction.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ProfileRepository profileRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String entityName, String entityId, AuditAction action, UUID performedById, String details) {
        try {
            Profile performedBy = null;
            if (performedById != null) {
                performedBy = profileRepository.findById(performedById).orElse(null);
            }

            AuditLog logEntry = AuditLog.builder()
                    .entityName(entityName)
                    .entityId(entityId)
                    .action(action)
                    .performedBy(performedBy)
                    .details(details)
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(logEntry);
            log.debug("Audit recorded: {} [{}] by {}", action, entityName, performedById);
        } catch (Exception e) {
            log.error("Failed to record audit log for {} {}: {}", entityName, entityId, e.getMessage());
        }
    }

    public void recordCurrent(String entityName, String entityId, AuditAction action, String details) {
        UUID currentUserId = null;
        try {
            currentUserId = SecurityUtil.getCurrentUserId();
        } catch (Exception ignored) {
            // Unauthenticated or system operation
        }
        record(entityName, entityId, action, currentUserId, details);
    }
}
