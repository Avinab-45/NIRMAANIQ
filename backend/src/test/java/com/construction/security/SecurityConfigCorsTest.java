package com.construction.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigCorsTest {

    @Test
    @DisplayName("CORS configuration source allows explicitly configured origins and disallows wildcards with credentials")
    void corsConfiguration_ExplicitAllowedOrigins_ConfiguredCorrectly() {
        SecurityConfig securityConfig = new SecurityConfig(null, null);

        // Given configured explicit origins including a stray wildcard
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins",
                "http://localhost:3000, http://localhost:5173, https://app.nirmaaniq.com, *");

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertNotNull(source);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/projects");
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertNotNull(config);
        List<String> origins = config.getAllowedOrigins();
        assertNotNull(origins);

        // Verify explicit origins are present
        assertTrue(origins.contains("http://localhost:3000"));
        assertTrue(origins.contains("http://localhost:5173"));
        assertTrue(origins.contains("https://app.nirmaaniq.com"));

        // Verify wildcard '*' is filtered out when credentials are true
        assertFalse(origins.contains("*"), "Wildcard origin must not be allowed when allowCredentials is true");

        // Verify credentials allowed
        assertTrue(Boolean.TRUE.equals(config.getAllowCredentials()));

        // Verify standard allowed methods
        assertTrue(config.getAllowedMethods().containsAll(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")));

        // Verify allowed headers
        assertTrue(config.getAllowedHeaders().containsAll(List.of("Authorization", "Content-Type", "Accept")));
    }
}
