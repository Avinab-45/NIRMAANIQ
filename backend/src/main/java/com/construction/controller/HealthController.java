package com.construction.controller;

import com.construction.dto.auth.HealthResponse;
import com.construction.dto.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.application.name:construction-backend}")
    private String appName;

    @GetMapping
    public ResponseEntity<ApiResponse<HealthResponse>> healthCheck() {
        boolean dbConnected = false;
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            dbConnected = (result != null && result == 1);
        } catch (Exception e) {
            log.error("Database connection check failed: {}", e.getMessage());
            dbConnected = false;
        }

        if (dbConnected) {
            HealthResponse health = HealthResponse.builder()
                    .status("UP")
                    .service(appName)
                    .version("1.0.0")
                    .timestamp(LocalDateTime.now())
                    .database("Supabase PostgreSQL (Connected)")
                    .build();
            return ResponseEntity.ok(ApiResponse.success("Service is running smoothly", health));
        } else {
            HealthResponse health = HealthResponse.builder()
                    .status("DOWN")
                    .service(appName)
                    .version("1.0.0")
                    .timestamp(LocalDateTime.now())
                    .database("Supabase PostgreSQL (Disconnected)")
                    .build();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.<HealthResponse>builder()
                            .success(false)
                            .message("Database connection is unavailable")
                            .data(health)
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
}
