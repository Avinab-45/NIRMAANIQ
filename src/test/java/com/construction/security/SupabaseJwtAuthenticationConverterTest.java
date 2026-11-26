package com.construction.security;

import com.construction.entity.Profile;
import com.construction.entity.Role;
import com.construction.entity.UserRole;
import com.construction.entity.enums.AccountStatus;
import com.construction.entity.enums.RoleType;
import com.construction.exception.UnauthorizedException;
import com.construction.repository.ProfileRepository;
import com.construction.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupabaseJwtAuthenticationConverterTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private SupabaseEmailVerificationService emailVerificationService;

    @InjectMocks
    private SupabaseJwtAuthenticationConverter converter;

    private Jwt createMockJwt(String sub, String email, Boolean emailVerified) {
        Map<String, Object> headers = Map.of("alg", "RS256", "typ", "JWT");
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("sub", sub);
        if (email != null) {
            claims.put("email", email);
        }
        if (emailVerified != null) {
            claims.put("email_verified", emailVerified);
        }

        return new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
    }

    @Test
    @DisplayName("Valid JWT with verified email successfully converts to authenticated UserPrincipal")
    void convert_ValidJwt_Success() {
        UUID userId = UUID.randomUUID();
        String email = "client@example.com";
        Jwt jwt = createMockJwt(userId.toString(), email, true);

        Profile profile = Profile.builder()
                .id(userId)
                .email(email)
                .status(AccountStatus.ACTIVE)
                .build();

        Role clientRole = Role.builder().name(RoleType.ROLE_CLIENT).build();
        UserRole userRole = UserRole.builder().user(profile).role(clientRole).build();

        doNothing().when(emailVerificationService).verifyEmail(jwt, userId, email);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(userRoleRepository.findByUser_Id(userId)).thenReturn(List.of(userRole));

        AbstractAuthenticationToken auth = converter.convert(jwt);

        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertEquals(userId, principal.getId());
        assertEquals(email, principal.getEmail());
        assertTrue(principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT")));
        verify(emailVerificationService).verifyEmail(jwt, userId, email);
    }

    @Test
    @DisplayName("JWT missing email claim throws UnauthorizedException")
    void convert_MissingEmail_ThrowsUnauthorized() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = createMockJwt(userId.toString(), null, null);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> converter.convert(jwt));
        assertTrue(ex.getMessage().contains("missing verified email"));
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    @DisplayName("JWT with unverified email throws UnauthorizedException")
    void convert_UnverifiedEmail_ThrowsUnauthorized() {
        UUID userId = UUID.randomUUID();
        String email = "unverified@example.com";
        Jwt jwt = createMockJwt(userId.toString(), email, false);

        doThrow(new UnauthorizedException("Email address is not verified in Supabase Auth"))
                .when(emailVerificationService).verifyEmail(jwt, userId, email);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> converter.convert(jwt));
        assertTrue(ex.getMessage().contains("not verified in Supabase Auth"));
        verify(profileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Email conflict with a different existing UUID throws UnauthorizedException")
    void convert_EmailConflictDifferentUuid_ThrowsUnauthorized() {
        UUID newUserId = UUID.randomUUID();
        UUID existingUserId = UUID.randomUUID();
        String email = "duplicate@example.com";
        Jwt jwt = createMockJwt(newUserId.toString(), email, true);

        Profile existingProfile = Profile.builder()
                .id(existingUserId)
                .email(email)
                .status(AccountStatus.ACTIVE)
                .build();

        doNothing().when(emailVerificationService).verifyEmail(jwt, newUserId, email);
        when(profileRepository.findById(newUserId)).thenReturn(Optional.empty());
        when(profileRepository.findByEmail(email)).thenReturn(Optional.of(existingProfile));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> converter.convert(jwt));
        assertTrue(ex.getMessage().contains("already associated with another account"));
        verify(profileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Missing sub claim in JWT throws UnauthorizedException")
    void convert_MissingSub_ThrowsUnauthorized() {
        Jwt jwt = createMockJwt(null, "user@example.com", true);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> converter.convert(jwt));
        assertTrue(ex.getMessage().contains("missing subject"));
    }

    @Test
    @DisplayName("Invalid UUID sub claim in JWT throws UnauthorizedException")
    void convert_InvalidSub_ThrowsUnauthorized() {
        Jwt jwt = createMockJwt("invalid-uuid-string", "user@example.com", true);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> converter.convert(jwt));
        assertTrue(ex.getMessage().contains("valid UUID"));
    }
}
