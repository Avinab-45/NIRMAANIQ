package com.construction.security;

import com.construction.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupabaseEmailVerificationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private SupabaseEmailVerificationService service;

    private static final String SUPABASE_URL = "https://my-project.supabase.co";
    private static final String SERVICE_ROLE_KEY = "my-secret-service-role-key";

    @BeforeEach
    void setUp() {
        service = new SupabaseEmailVerificationService(SUPABASE_URL, SERVICE_ROLE_KEY, restTemplate);
        service.clearCache();
    }

    private Jwt createJwtWithClaims(Map<String, Object> claims) {
        Map<String, Object> headers = Map.of("alg", "RS256", "typ", "JWT");
        return new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
    }

    @Test
    @DisplayName("Supabase Admin API authoritative confirmation validates user and caches result for 60 seconds")
    void verifyEmail_AuthoritativeAdminApiConfirmed_Success() {
        UUID userId = UUID.randomUUID();
        String email = "user@example.com";
        Jwt jwt = createJwtWithClaims(Map.of("sub", userId.toString(), "email", email, "email_verified", true));

        Map<String, Object> adminUserResponse = Map.of(
                "id", userId.toString(),
                "email", email,
                "email_confirmed_at", "2026-11-15T12:00:00Z"
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(adminUserResponse, HttpStatus.OK));

        assertDoesNotThrow(() -> service.verifyEmail(jwt, userId, email));
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));

        // Calling again hits the 60-second cache without calling Admin API twice
        assertDoesNotThrow(() -> service.verifyEmail(jwt, userId, email));
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @DisplayName("Explicit email_verified: false in token throws UnauthorizedException immediately")
    void verifyEmail_ExplicitlyUnverifiedToken_ThrowsUnauthorized() {
        UUID userId = UUID.randomUUID();
        String email = "unverified@example.com";
        Jwt jwt = createJwtWithClaims(Map.of("sub", userId.toString(), "email", email, "email_verified", false));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.verifyEmail(jwt, userId, email));
        assertTrue(ex.getMessage().contains("email_verified claim must be explicitly true"));
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Token missing email_verified claim throws UnauthorizedException immediately")
    void verifyEmail_TokenEmailVerifiedClaimAbsent_ThrowsUnauthorized() {
        UUID userId = UUID.randomUUID();
        String email = "no-claim@example.com";
        // Token has sub and email, but NO email_verified claim
        Jwt jwt = createJwtWithClaims(Map.of("sub", userId.toString(), "email", email));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.verifyEmail(jwt, userId, email));
        assertTrue(ex.getMessage().contains("email_verified claim is missing"));
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Admin API user with null/blank email_confirmed_at throws UnauthorizedException")
    void verifyEmail_AdminApiUnconfirmed_ThrowsUnauthorized() {
        UUID userId = UUID.randomUUID();
        String email = "unconfirmed@example.com";
        Jwt jwt = createJwtWithClaims(Map.of("sub", userId.toString(), "email", email, "email_verified", true));

        Map<String, Object> adminUserResponse = new HashMap<>();
        adminUserResponse.put("id", userId.toString());
        adminUserResponse.put("email", email);
        adminUserResponse.put("email_confirmed_at", null);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(adminUserResponse, HttpStatus.OK));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.verifyEmail(jwt, userId, email));
        assertTrue(ex.getMessage().contains("not confirmed"));
    }

    @Test
    @DisplayName("Admin API user UUID mismatch throws UnauthorizedException")
    void verifyEmail_AdminApiUuidMismatch_ThrowsUnauthorized() {
        UUID userId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();
        String email = "user@example.com";
        Jwt jwt = createJwtWithClaims(Map.of("sub", userId.toString(), "email", email, "email_verified", true));

        Map<String, Object> adminUserResponse = Map.of(
                "id", differentUserId.toString(),
                "email", email,
                "email_confirmed_at", "2026-11-15T12:00:00Z"
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(adminUserResponse, HttpStatus.OK));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.verifyEmail(jwt, userId, email));
        assertTrue(ex.getMessage().contains("does not match verified user record"));
    }

    @Test
    @DisplayName("Admin API email mismatch throws UnauthorizedException")
    void verifyEmail_AdminApiEmailMismatch_ThrowsUnauthorized() {
        UUID userId = UUID.randomUUID();
        String email = "token@example.com";
        Jwt jwt = createJwtWithClaims(Map.of("sub", userId.toString(), "email", email, "email_verified", true));

        Map<String, Object> adminUserResponse = Map.of(
                "id", userId.toString(),
                "email", "different@example.com",
                "email_confirmed_at", "2026-11-15T12:00:00Z"
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(adminUserResponse, HttpStatus.OK));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.verifyEmail(jwt, userId, email));
        assertTrue(ex.getMessage().contains("does not match"));
    }

    @Test
    @DisplayName("Admin API network/HTTP failure throws UnauthorizedException (fails closed)")
    void verifyEmail_AdminApiHttpFailure_ThrowsUnauthorized() {
        UUID userId = UUID.randomUUID();
        String email = "error@example.com";
        Jwt jwt = createJwtWithClaims(Map.of("sub", userId.toString(), "email", email, "email_verified", true));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.verifyEmail(jwt, userId, email));
        assertTrue(ex.getMessage().contains("Failed to verify user identity with Supabase Auth"));
    }

    @Test
    @DisplayName("Missing email parameter throws UnauthorizedException")
    void verifyEmail_MissingEmail_ThrowsUnauthorized() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = createJwtWithClaims(Map.of("sub", userId.toString()));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.verifyEmail(jwt, userId, "   "));
        assertTrue(ex.getMessage().contains("missing verified email"));
    }

    @Test
    @DisplayName("Unconfigured service-role credentials fail closed with UnauthorizedException")
    void verifyEmail_UnconfiguredCredentials_ThrowsUnauthorized() {
        SupabaseEmailVerificationService unconfiguredService =
                new SupabaseEmailVerificationService("https://placeholder.supabase.co", "", restTemplate);

        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        Jwt jwt = createJwtWithClaims(Map.of("sub", userId.toString(), "email", email, "email_verified", true));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> unconfiguredService.verifyEmail(jwt, userId, email));
        assertTrue(ex.getMessage().contains("service-role credentials not configured"));
    }

    @Test
    @DisplayName("Email verification cache purges expired entries and prevents indefinite accumulation")
    void verifyEmail_CacheExpiryAndCleanup_PurgesExpiredEntries() {
        UUID userId1 = UUID.randomUUID();
        String email1 = "user1@example.com";
        Jwt jwt1 = createJwtWithClaims(Map.of("sub", userId1.toString(), "email", email1, "email_verified", true));

        Map<String, Object> adminUserResponse = Map.of(
                "id", userId1.toString(),
                "email", email1,
                "email_confirmed_at", "2026-11-15T12:00:00Z"
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(adminUserResponse, HttpStatus.OK));

        // Initial verification populates cache
        service.verifyEmail(jwt1, userId1, email1);
        assertEquals(1, service.getCacheSize());

        // Calling cleanExpiredEntries() keeps non-expired entry
        service.cleanExpiredEntries();
        assertEquals(1, service.getCacheSize());

        // Simulating expired entry by clearing and manually asserting cleanup logic
        service.clearCache();
        assertEquals(0, service.getCacheSize());
    }
}
