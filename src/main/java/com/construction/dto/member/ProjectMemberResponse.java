package com.construction.dto.member;

import com.construction.entity.enums.MemberStatus;
import com.construction.entity.enums.ProjectMemberRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemberResponse {
    private UUID id;
    private UUID userId;
    private String userEmail;
    private String userFullName;
    private String userPhone;
    private ProjectMemberRole role;
    private MemberStatus status;
    private LocalDateTime joinedAt;
}
