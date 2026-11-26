package com.construction.dto.auth;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthResponse {
    private String status;
    private String service;
    private String version;
    private LocalDateTime timestamp;
    private String database;
}
