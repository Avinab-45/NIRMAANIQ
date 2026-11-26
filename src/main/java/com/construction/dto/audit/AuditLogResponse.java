package com.construction.dto.audit;

import com.construction.entity.enums.AuditAction;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private UUID id;
    private String entityName;
    private String entityId;
    private AuditAction action;
    private UUID performedById;
    private String performedByName;
    private String details;
    private String ipAddress;
    private LocalDateTime timestamp;
}
