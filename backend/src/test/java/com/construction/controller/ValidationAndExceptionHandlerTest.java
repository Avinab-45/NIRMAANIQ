package com.construction.controller;

import com.construction.dto.auth.BuilderActivationRequest;
import com.construction.dto.auth.ContractorOtpRequest;
import com.construction.dto.project.CreateProjectRequest;
import com.construction.exception.ForbiddenException;
import com.construction.exception.GlobalExceptionHandler;
import com.construction.exception.ResourceNotFoundException;
import com.construction.security.SupabaseJwtAuthenticationConverter;
import com.construction.service.AuthService;
import com.construction.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = {HealthController.class, AuthController.class, ProjectController.class},
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ValidationAndExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @MockBean
    private SupabaseJwtAuthenticationConverter supabaseJwtAuthenticationConverter;

    @MockBean
    private AuthService authService;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Health endpoint with connected database returns 200 UP")
    void healthCheck_ReturnsUp() throws Exception {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.database").value("Supabase PostgreSQL (Connected)"));
    }

    @Test
    @DisplayName("Health endpoint with disconnected database returns 503 Service Unavailable")
    void healthCheck_DatabaseDown_ReturnsServiceUnavailable() throws Exception {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new org.springframework.jdbc.CannotGetJdbcConnectionException("Connection refused"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.status").value("DOWN"))
                .andExpect(jsonPath("$.data.database").value("Supabase PostgreSQL (Disconnected)"));
    }

    @Test
    @DisplayName("Invalid email format in OTP request triggers 400 Bad Request with validation errors")
    void contractorOtpRequest_InvalidEmail_ReturnsBadRequest() throws Exception {
        ContractorOtpRequest invalidRequest = ContractorOtpRequest.builder()
                .email("not-an-email")
                .build();

        mockMvc.perform(post("/api/auth/contractor/request-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("Empty project name in project creation triggers 400 Bad Request")
    void createProject_BlankName_ReturnsBadRequest() throws Exception {
        CreateProjectRequest invalidRequest = CreateProjectRequest.builder()
                .name("")
                .build();

        mockMvc.perform(post("/api/projects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.validationErrors.name").exists());
    }

    @Test
    @DisplayName("ResourceNotFoundException handled by GlobalExceptionHandler returns 404")
    void getProject_NotFound_Returns404() throws Exception {
        when(projectService.getProjectByCode("PRJ-NOTFOUND"))
                .thenThrow(new ResourceNotFoundException("Project not found with code: PRJ-NOTFOUND"));

        mockMvc.perform(get("/api/projects/PRJ-NOTFOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Project not found with code: PRJ-NOTFOUND"));
    }

    @Test
    @DisplayName("ForbiddenException handled by GlobalExceptionHandler returns 403")
    void activateBuilder_Forbidden_Returns403() throws Exception {
        when(authService.activateBuilder(any()))
                .thenThrow(new ForbiddenException("Invalid builder invitation/activation code."));

        BuilderActivationRequest request = BuilderActivationRequest.builder()
                .activationCode("INVALID-CODE")
                .build();

        mockMvc.perform(post("/api/auth/builder/activate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Invalid builder invitation/activation code."));
    }
}
