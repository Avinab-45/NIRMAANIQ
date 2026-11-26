package com.construction.dto.auth;

import com.construction.entity.enums.AccountStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private AccountStatus status;
    private String avatarUrl;
    private List<String> roles;
    private LocalDateTime createdAt;
}
