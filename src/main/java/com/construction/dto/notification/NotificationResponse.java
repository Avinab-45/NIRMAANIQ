package com.construction.dto.notification;

import com.construction.entity.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private UUID id;
    private String title;
    private String message;
    private NotificationType type;
    private boolean isRead;
    private String projectCode;
    private LocalDateTime createdAt;
}
