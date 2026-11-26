package com.construction.service;

import com.construction.dto.notification.NotificationResponse;
import com.construction.entity.Notification;
import com.construction.entity.Profile;
import com.construction.entity.Project;
import com.construction.entity.enums.NotificationType;
import com.construction.exception.ResourceNotFoundException;
import com.construction.repository.NotificationRepository;
import com.construction.repository.ProfileRepository;
import com.construction.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ProfileRepository profileRepository;

    @Transactional
    public void createNotification(UUID recipientId, Project project, String title, String message, NotificationType type) {
        Profile recipient = profileRepository.findById(recipientId).orElse(null);
        if (recipient == null) {
            log.warn("Cannot create notification: recipient {} not found", recipientId);
            return;
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .project(project)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Pageable pageable) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        return notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(currentUserId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getRecipient().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Notification not found with id: " + notificationId);
        }

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return mapToResponse(saved);
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .isRead(n.isRead())
                .projectCode(n.getProject() != null ? n.getProject().getProjectCode() : null)
                .createdAt(n.getCreatedAt())
                .build();
    }
}
