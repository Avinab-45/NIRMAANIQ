package com.construction.controller;

import com.construction.dto.auth.HealthResponse;
import com.construction.dto.common.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private HealthController healthController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(healthController, "appName", "construction-backend");
    }

    @Test
    @DisplayName("Health check with connected database (SELECT 1 returns 1) returns HTTP 200 and UP status")
    void healthCheck_DatabaseConnected_ReturnsHttp200AndUp() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);

        ResponseEntity<ApiResponse<HealthResponse>> response = healthController.healthCheck();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("UP", response.getBody().getData().getStatus());
        assertEquals("Supabase PostgreSQL (Connected)", response.getBody().getData().getDatabase());
        assertEquals("construction-backend", response.getBody().getData().getService());
    }

    @Test
    @DisplayName("Health check with database connection failure throws exception, returns HTTP 503 and DOWN status")
    void healthCheck_DatabaseDisconnected_ReturnsHttp503AndDown() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class)))
                .thenThrow(new DataAccessResourceFailureException("Unable to acquire connection from pool"));

        ResponseEntity<ApiResponse<HealthResponse>> response = healthController.healthCheck();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("DOWN", response.getBody().getData().getStatus());
        assertEquals("Supabase PostgreSQL (Disconnected)", response.getBody().getData().getDatabase());
    }

    @Test
    @DisplayName("Health check when SELECT 1 returns null returns HTTP 503 and DOWN status")
    void healthCheck_DatabaseQueryReturnsNull_ReturnsHttp503AndDown() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(null);

        ResponseEntity<ApiResponse<HealthResponse>> response = healthController.healthCheck();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("DOWN", response.getBody().getData().getStatus());
    }
}
