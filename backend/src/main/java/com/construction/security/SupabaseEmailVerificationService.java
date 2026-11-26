package com.construction.security;

import com.construction.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for validating that authenticated users have confirmed/verified emails in Supabase Auth.
 * Uses the Supabase Admin API as the authoritative source of email confirmation.
 * Never trusts client/frontend-supplied verification flags, AMR claims, or user metadata.
 * Employs a bounded 60-second cache strictly for successful verification results.
 */
@Slf4j
@Service
public class SupabaseEmailVerificationService {

    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final RestTemplate restTemplate;

    // Short, bounded cache (maximum 60 seconds) strictly for successful verification results
    private final Map<String, Instant> verifiedCache = new ConcurrentHashMap<>();
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    @Autowired
    public SupabaseEmailVerificationService(
            @Value("${app.supabase.url:https://placeholder.supabase.co}") String supabaseUrl,
            @Value("${app.supabase.service-role-key:}") String serviceRoleKey,
            RestTemplateBuilder restTemplateBuilder) {
        this.supabaseUrl = supabaseUrl != null ? supabaseUrl.trim() : "";
        this.serviceRoleKey = serviceRoleKey != null ? serviceRoleKey.trim() : "";
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    // Constructor for testing
    public SupabaseEmailVerificationService(String supabaseUrl, String serviceRoleKey, RestTemplate restTemplate) {
        this.supabaseUrl = supabaseUrl != null ? supabaseUrl.trim() : "";
        this.serviceRoleKey = serviceRoleKey != null ? serviceRoleKey.trim() : "";
        this.restTemplate = restTemplate;
    }

    /**
     * Verifies that the user's email address is confirmed and belongs to the authenticated UUID.
     * Uses the Supabase Admin API as the authoritative source of truth.
     *
     * @param jwt             Decoded JWT token
     * @param userId          Authenticated UUID from sub claim
     * @param normalizedEmail Lowercase email address from token claim
     * @throws UnauthorizedException if email is missing, unverified, mismatched, or cannot be verified
     */
    public void verifyEmail(Jwt jwt, UUID userId, String normalizedEmail) {
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new UnauthorizedException("JWT missing verified email claim");
        }

        // 1. Require JWT email_verified claim to be explicitly true
        Object emailVerifiedClaim = jwt.getClaim("email_verified");
        if (emailVerifiedClaim == null) {
            log.warn("Rejected JWT for user {}: missing email_verified claim", userId);
            throw new UnauthorizedException("Email address is not verified: email_verified claim is missing");
        }
        boolean isExplicitlyTrue = (emailVerifiedClaim instanceof Boolean b && b)
                || (emailVerifiedClaim instanceof String s && "true".equalsIgnoreCase(s.trim()));
        if (!isExplicitlyTrue) {
            log.warn("Rejected JWT for user {}: email_verified claim is not true (found: {})", userId, emailVerifiedClaim);
            throw new UnauthorizedException("Email address is not verified: email_verified claim must be explicitly true");
        }

        // 2. Purge expired cache entries to prevent indefinite memory accumulation
        cleanExpiredEntries();

        // Check short, bounded cache (60 seconds) strictly for prior successful verification
        String cacheKey = userId + ":" + normalizedEmail;
        Instant cachedExpiry = verifiedCache.get(cacheKey);
        if (cachedExpiry != null && Instant.now().isBefore(cachedExpiry)) {
            log.debug("Cache hit for verified email user {}", userId);
            return; // Cache hit: positive verification confirmed recently
        }

        // 3. Query the authoritative Supabase Admin API
        verifyWithSupabaseAdminApi(userId, normalizedEmail, cacheKey);
    }

    private void verifyWithSupabaseAdminApi(UUID userId, String normalizedEmail, String cacheKey) {
        // Fail closed if Supabase verification credentials are not configured or placeholder
        if (supabaseUrl.isBlank() || supabaseUrl.contains("placeholder") ||
            serviceRoleKey.isBlank() || serviceRoleKey.contains("placeholder")) {
            log.error("Fail closed: Supabase Admin service-role credentials not configured for email verification");
            throw new UnauthorizedException("Server cannot verify email: Supabase Admin service-role credentials not configured.");
        }

        String adminUrl = supabaseUrl.replaceAll("/+$", "") + "/auth/v1/admin/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", serviceRoleKey);
        headers.setBearerAuth(serviceRoleKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    adminUrl,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> userData = response.getBody();

                // Verify user UUID matches
                Object returnedIdObj = userData.get("id");
                if (returnedIdObj == null || !userId.toString().equalsIgnoreCase(returnedIdObj.toString())) {
                    log.error("UUID mismatch in Supabase Auth: Token UUID {} does not match user UUID {}",
                            userId, returnedIdObj);
                    throw new UnauthorizedException("Token subject does not match verified user record");
                }

                // Verify email matches
                String returnedEmail = (String) userData.get("email");
                if (returnedEmail == null || !returnedEmail.trim().equalsIgnoreCase(normalizedEmail)) {
                    log.error("Email mismatch in Supabase Auth: Token email {} does not match user email {}",
                            normalizedEmail, returnedEmail);
                    throw new UnauthorizedException("Token email does not match verified user record");
                }

                // Verify confirmation timestamp is non-null and non-empty
                Object confirmedAt = userData.get("email_confirmed_at");
                if (confirmedAt == null) {
                    confirmedAt = userData.get("confirmed_at");
                }

                if (confirmedAt == null || confirmedAt.toString().isBlank()) {
                    log.warn("User {} ({}) has not confirmed their email in Supabase Auth", userId, normalizedEmail);
                    throw new UnauthorizedException("Email address " + normalizedEmail + " is not confirmed in Supabase Auth");
                }

                // Cache ONLY successful verification results with 60-second TTL
                verifiedCache.put(cacheKey, Instant.now().plus(CACHE_TTL));
                log.info("Successfully verified confirmed email for user {} via Supabase Admin API", userId);
            } else {
                log.warn("Supabase Admin API returned non-2xx status {} for user {}", response.getStatusCode(), userId);
                throw new UnauthorizedException("Failed to verify user email with Supabase Auth: User not found or invalid response");
            }
        } catch (RestClientException e) {
            log.error("Fail closed: Supabase Admin API error verifying user {}: {}", userId, e.getMessage());
            throw new UnauthorizedException("Failed to verify user identity with Supabase Auth: " + e.getMessage());
        }
    }

    /**
     * Purges expired entries from the cache to prevent unbounded growth over time.
     */
    public void cleanExpiredEntries() {
        Instant now = Instant.now();
        verifiedCache.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }

    /**
     * Package-private inspection of active cache size for testing and monitoring.
     */
    public int getCacheSize() {
        return verifiedCache.size();
    }

    /**
     * Clear the cache for testing or administration.
     */
    public void clearCache() {
        verifiedCache.clear();
    }
}
