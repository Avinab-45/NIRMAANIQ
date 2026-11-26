package com.construction.security;

import com.construction.entity.Profile;
import com.construction.entity.UserRole;
import com.construction.entity.enums.AccountStatus;
import com.construction.exception.UnauthorizedException;
import com.construction.repository.ProfileRepository;
import com.construction.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final ProfileRepository profileRepository;
    private final UserRoleRepository userRoleRepository;
    private final SupabaseEmailVerificationService emailVerificationService;

    @Override
    @Transactional
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new UnauthorizedException("JWT missing subject ('sub') claim");
        }

        UUID userId;
        try {
            userId = UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID in JWT sub claim: {}", sub);
            throw new UnauthorizedException("JWT subject must be a valid UUID");
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            log.warn("Rejected JWT for user {}: missing verified email claim", userId);
            throw new UnauthorizedException("JWT missing verified email claim");
        }

        String normalizedEmail = email.trim().toLowerCase();

        // Perform trusted email verification (in-token + server-side Supabase Admin API verification)
        emailVerificationService.verifyEmail(jwt, userId, normalizedEmail);

        String phone = jwt.getClaimAsString("phone");

        // Lookup profile in our database strictly by UUID
        Optional<Profile> profileOpt = profileRepository.findById(userId);
        Profile profile;

        if (profileOpt.isEmpty()) {
            // Guard: Check if another profile already has this email with a different UUID
            Optional<Profile> existingEmailProfile = profileRepository.findByEmail(normalizedEmail);
            if (existingEmailProfile.isPresent() && !existingEmailProfile.get().getId().equals(userId)) {
                log.error("Email conflict: Email {} is already linked to profile UUID {}",
                        normalizedEmail, existingEmailProfile.get().getId());
                throw new UnauthorizedException("Email is already associated with another account");
            }

            // Provision a pending profile bound strictly to the authenticated Supabase UUID
            profile = Profile.builder()
                    .id(userId)
                    .email(normalizedEmail)
                    .phoneNumber(phone)
                    .status(AccountStatus.PENDING_ACTIVATION)
                    .build();
            profile = profileRepository.save(profile);
            log.info("Provisioned initial profile for authenticated user: {} ({})", userId, normalizedEmail);
        } else {
            profile = profileOpt.get();
            boolean changed = false;

            // Sync email if updated in Supabase Auth, guarding against conflicts
            if (!normalizedEmail.equalsIgnoreCase(profile.getEmail())) {
                Optional<Profile> emailConflict = profileRepository.findByEmail(normalizedEmail);
                if (emailConflict.isPresent() && !emailConflict.get().getId().equals(userId)) {
                    log.error("Cannot sync email {}: already linked to UUID {}", normalizedEmail, emailConflict.get().getId());
                    throw new UnauthorizedException("Email is already associated with another account");
                }
                profile.setEmail(normalizedEmail);
                changed = true;
            }

            // Sync optional contact phone number if present
            if (phone != null && !phone.isBlank() && !phone.equalsIgnoreCase(profile.getPhoneNumber())) {
                profile.setPhoneNumber(phone);
                changed = true;
            }

            if (changed) {
                profile = profileRepository.save(profile);
            }
        }

        // Fetch user roles
        List<UserRole> userRoles = userRoleRepository.findByUser_Id(userId);
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (UserRole ur : userRoles) {
            authorities.add(new SimpleGrantedAuthority(ur.getRole().getName().name()));
        }

        UserPrincipal principal = UserPrincipal.builder()
                .id(userId)
                .email(profile.getEmail())
                .phoneNumber(profile.getPhoneNumber())
                .status(profile.getStatus())
                .authorities(authorities)
                .build();

        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }
}
