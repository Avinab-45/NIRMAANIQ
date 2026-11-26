package com.construction.service;

import com.construction.config.MlProperties;
import com.construction.security.SupabaseEmailVerificationService;
import com.construction.service.ai.AiVisionClient;
import com.construction.service.ai.GeminiVisionClient;
import com.construction.service.ml.MlAnalysisService;
import com.construction.service.ml.MlServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class SupabaseServiceInstantiationTest {

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    @Autowired
    private SupabaseEmailVerificationService supabaseEmailVerificationService;

    @Autowired
    private AiService aiService;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private AiVisionClient aiVisionClient;

    @Autowired
    private MlServiceClient mlServiceClient;

    @Autowired
    private MlAnalysisService mlAnalysisService;

    @Autowired
    private MlProperties mlProperties;

    @Test
    @DisplayName("SupabaseStorageService, ML services, and dependent services instantiate cleanly in Spring ApplicationContext")
    void contextLoads_SupabaseServicesSuccessfullyInstantiated() {
        assertNotNull(supabaseStorageService, "SupabaseStorageService must be registered and injected");
        assertNotNull(supabaseEmailVerificationService, "SupabaseEmailVerificationService must be registered and injected");
        assertNotNull(aiService, "AiService must be registered and injected");
        assertNotNull(mediaService, "MediaService must be registered and injected");
        assertNotNull(aiVisionClient, "AiVisionClient must be registered and injected");
        assertTrue(aiVisionClient instanceof GeminiVisionClient, "AiVisionClient should be an instance of GeminiVisionClient");
        assertNotNull(mlServiceClient, "MlServiceClient must be registered and injected");
        assertNotNull(mlAnalysisService, "MlAnalysisService must be registered and injected");
        assertNotNull(mlProperties, "MlProperties must be registered and injected");
    }
}
