package com.construction.service;

import com.construction.config.BuilderProperties;
import com.construction.dto.auth.*;
import com.construction.entity.OtpChallenge;
import com.construction.entity.Profile;
import com.construction.entity.Role;
import com.construction.entity.UserRole;
import com.construction.entity.enums.AccountStatus;
import com.construction.entity.enums.AuditAction;
import com.construction.entity.enums.MemberStatus;
import com.construction.entity.enums.ProjectMemberRole;
import com.construction.entity.enums.RoleType;
import com.construction.exception.BadRequestException;
import com.construction.exception.ForbiddenException;
import com.construction.exception.RateLimitException;
import com.construction.exception.ResourceNotFoundException;
import com.construction.repository.OtpChallengeRepository;
import com.construction.repository.ProfileRepository;
import com.construction.repository.ProjectMemberRepository;
import com.construction.repository.RoleRepository;
import com.construction.repository.UserRoleRepository;
import com.construction.security.SecurityUtil;
import com.construction.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final ProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final OtpChallengeRepository otpChallengeRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final BuilderProperties builderProperties;

    @Value("${app.contractor.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${app.contractor.otp.max-attempts:5}")
    private int otpMaxAttempts;

    @Value("${app.contractor.otp.resend-interval-seconds:60}")
    private int otpResendIntervalSeconds;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Activates a Builder account after verified email and valid invitation/approval code.
     * Assigns ROLE_BUILDER and sets status to ACTIVE.
     * Enforces explicit, non-default BUILDER_ACTIVATION_CODE.
     */
    @Transactional
    public UserProfileResponse activateBuilder(BuilderActivationRequest request) {
        UserPrincipal userPrincipal = SecurityUtil.getCurrentUser();
        UUID userId = userPrincipal.getId();

        // Verify activation code against non-default configured secret
        String expectedActivationCode = builderProperties.getActivationCode();
        if (expectedActivationCode == null || !expectedActivationCode.equals(request.getActivationCode())) {
            auditLogService.record("profiles", userId.toString(), AuditAction.ACTIVATE, userId,
                    "Failed builder activation attempt: invalid code");
            throw new ForbiddenException("Invalid builder invitation/activation code.");
        }

        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        if (profile.getStatus() == AccountStatus.SUSPENDED) {
            throw new ForbiddenException("Account is suspended.");
        }

        // Activate profile
        profile.setStatus(AccountStatus.ACTIVE);
        profile = profileRepository.save(profile);

        // Assign ROLE_BUILDER if not already assigned
        Role builderRole = roleRepository.findByName(RoleType.ROLE_BUILDER)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(RoleType.ROLE_BUILDER)
                        .description("Master builder with project creation and management rights")
                        .build()));

        if (!userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)) {
            UserRole userRole = UserRole.builder()
                    .user(profile)
                    .role(builderRole)
                    .assignedAt(LocalDateTime.now())
                    .build();
            userRoleRepository.save(userRole);
        }

        auditLogService.record("profiles", userId.toString(), AuditAction.ACTIVATE, userId,
                "Builder account successfully activated with ROLE_BUILDER");

        return getProfileResponse(profile);
    }

    /**
     * Returns the currently authenticated Builder profile.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getBuilderProfile() {
        SecurityUtil.requireRole(RoleType.ROLE_BUILDER);
        UUID userId = SecurityUtil.getCurrentUserId();
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));
        return getProfileResponse(profile);
    }

    /**
     * Contractor requests a 6-digit email OTP for secondary verification.
     * Enforces strict authorization: user must have an invitation from a Builder
     * or existing ROLE_CONTRACTOR. Builders, Clients, uninvited users, and suspended accounts are rejected.
     */
    @Transactional
    public void requestContractorOtp(ContractorOtpRequest request) {
        UserPrincipal currentUser = SecurityUtil.getCurrentUser();
        UUID userId = currentUser.getId();
        String authenticatedEmail = currentUser.getEmail().toLowerCase().trim();

        // 1. Verify account is not suspended
        if (currentUser.getStatus() == AccountStatus.SUSPENDED) {
            throw new ForbiddenException("Account is suspended.");
        }

        // 2. Prevent conflicting roles: Builder or Client cannot request Contractor OTP
        if (userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER) ||
            userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CLIENT)) {
            log.warn("Security violation: User {} with existing Builder/Client role attempted contractor OTP request", userId);
            throw new ForbiddenException("Users registered as Builder or Client cannot activate as a Contractor.");
        }

        // 3. Verify server-side Contractor invitation or approval (requires ACTIVE invitation)
        boolean isInvited = projectMemberRepository.existsByUser_IdAndRoleAndStatus(
                userId, ProjectMemberRole.CONTRACTOR, MemberStatus.ACTIVE);
        boolean hasContractorRole = userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CONTRACTOR);

        if (!isInvited && !hasContractorRole) {
            log.warn("Security rejection: User {} ({}) has not been invited or approved as an active Contractor", userId, authenticatedEmail);
            throw new ForbiddenException("You do not have an active invitation or approval as a Contractor by any Builder.");
        }

        // 4. Verify that any supplied email matches the authenticated user's account
        if (request != null && request.getEmail() != null && !request.getEmail().isBlank()) {
            String suppliedEmail = request.getEmail().toLowerCase().trim();
            if (!suppliedEmail.equalsIgnoreCase(authenticatedEmail)) {
                log.warn("Security violation: User {} ({}) attempted to request OTP for email {}",
                        userId, authenticatedEmail, suppliedEmail);
                throw new ForbiddenException("Cannot request OTP for an email different from your authenticated account.");
            }
        }

        // 5. Check rate limiting (resend cooldown)
        Optional<OtpChallenge> latestChallenge = otpChallengeRepository
                .findFirstByEmailAndPurposeOrderByCreatedAtDesc(authenticatedEmail, "CONTRACTOR_LOGIN");

        if (latestChallenge.isPresent()) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cooldownUntil = latestChallenge.get().getCreatedAt().plusSeconds(otpResendIntervalSeconds);
            if (now.isBefore(cooldownUntil)) {
                long remainingSeconds = java.time.Duration.between(now, cooldownUntil).getSeconds();
                throw new RateLimitException("Please wait " + remainingSeconds + " seconds before requesting a new OTP.");
            }
        }

        // 6. Generate cryptographically secure 6-digit OTP
        int otpNumber = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(otpNumber);

        // Hash the OTP using SHA-256 with email salt
        String otpHash = hashOtp(otpCode, authenticatedEmail);

        // Save challenge
        OtpChallenge challenge = OtpChallenge.builder()
                .email(authenticatedEmail)
                .otpHash(otpHash)
                .purpose("CONTRACTOR_LOGIN")
                .attemptsCount(0)
                .maxAttempts(otpMaxAttempts)
                .isUsed(false)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .build();

        if (challenge.getId() == null) {
            challenge.setId(UUID.randomUUID());
        }
        OtpChallenge savedChallenge = otpChallengeRepository.save(challenge);
        String challengeId = (savedChallenge != null && savedChallenge.getId() != null)
                ? savedChallenge.getId().toString()
                : challenge.getId().toString();

        // Send OTP via email (failures propagate instead of silent suppression)
        emailService.sendOtpEmail(authenticatedEmail, otpCode);

        auditLogService.record("otp_challenges", challengeId, AuditAction.CREATE, userId,
                "Contractor OTP requested for email: " + authenticatedEmail);
    }

    /**
     * Contractor verifies the 6-digit email OTP.
     * Re-verifies approval, prevents role escalation, validates attempts, hash match, and expiry.
     * Updates the existing authenticated profile and assigns ROLE_CONTRACTOR.
     */
    @Transactional
    public UserProfileResponse verifyContractorOtp(ContractorVerifyOtpRequest request) {
        UserPrincipal currentUser = SecurityUtil.getCurrentUser();
        UUID userId = currentUser.getId();
        String authenticatedEmail = currentUser.getEmail().toLowerCase().trim();
        String otp = request.getOtp().trim();

        // 1. Verify account is not suspended
        if (currentUser.getStatus() == AccountStatus.SUSPENDED) {
            throw new ForbiddenException("Account is suspended.");
        }

        // 2. Prevent conflicting roles
        if (userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER) ||
            userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CLIENT)) {
            throw new ForbiddenException("Users registered as Builder or Client cannot activate as a Contractor.");
        }

        // 3. Verify server-side Contractor invitation or approval (requires ACTIVE invitation)
        boolean isInvited = projectMemberRepository.existsByUser_IdAndRoleAndStatus(
                userId, ProjectMemberRole.CONTRACTOR, MemberStatus.ACTIVE);
        boolean hasContractorRole = userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CONTRACTOR);

        if (!isInvited && !hasContractorRole) {
            throw new ForbiddenException("You do not have an active invitation or approval as a Contractor by any Builder.");
        }

        // 4. Verify that the email matches the authenticated account
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String suppliedEmail = request.getEmail().toLowerCase().trim();
            if (!suppliedEmail.equalsIgnoreCase(authenticatedEmail)) {
                log.warn("Security violation: User {} ({}) attempted to verify OTP for email {}",
                        userId, authenticatedEmail, suppliedEmail);
                throw new ForbiddenException("Cannot verify OTP for another user's identity.");
            }
        }

        OtpChallenge challenge = otpChallengeRepository
                .findFirstByEmailAndPurposeAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        authenticatedEmail, "CONTRACTOR_LOGIN", LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("No active OTP challenge found or OTP has expired. Please request a new OTP."));

        if (challenge.getAttemptsCount() >= challenge.getMaxAttempts()) {
            challenge.setUsed(true);
            otpChallengeRepository.save(challenge);
            auditLogService.record("otp_challenges", challenge.getId().toString(), AuditAction.VERIFY, userId,
                    "OTP challenge locked out due to exceeding max attempts for email: " + authenticatedEmail);
            throw new BadRequestException("Maximum verification attempts exceeded. Please request a new OTP.");
        }

        // Increment attempts count
        challenge.setAttemptsCount(challenge.getAttemptsCount() + 1);

        String expectedHash = hashOtp(otp, authenticatedEmail);
        if (!MessageDigest.isEqual(challenge.getOtpHash().getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8))) {
            otpChallengeRepository.save(challenge);
            int remaining = challenge.getMaxAttempts() - challenge.getAttemptsCount();
            throw new BadRequestException("Invalid OTP. Remaining attempts: " + remaining);
        }

        // OTP verified successfully - mark challenge as used (one-time use)
        challenge.setUsed(true);
        otpChallengeRepository.save(challenge);

        // Retrieve the authenticated user's profile strictly by UUID
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for authenticated user: " + userId));

        profile.setStatus(AccountStatus.ACTIVE);
        profile = profileRepository.save(profile);

        // Assign ROLE_CONTRACTOR
        Role contractorRole = roleRepository.findByName(RoleType.ROLE_CONTRACTOR)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(RoleType.ROLE_CONTRACTOR)
                        .description("Contractor with operational project management rights")
                        .build()));

        if (!userRoleRepository.existsByUser_IdAndRole_Name(profile.getId(), RoleType.ROLE_CONTRACTOR)) {
            UserRole userRole = UserRole.builder()
                    .user(profile)
                    .role(contractorRole)
                    .assignedAt(LocalDateTime.now())
                    .build();
            userRoleRepository.save(userRole);
        }

        auditLogService.record("otp_challenges", challenge.getId().toString(), AuditAction.VERIFY, profile.getId(),
                "Contractor OTP successfully verified for email: " + authenticatedEmail);

        return getProfileResponse(profile);
    }

    /**
     * Returns current authenticated Client profile using the verified Supabase Email OTP identity.
     * Prevents role escalation: Builder or Contractor users cannot obtain ROLE_CLIENT.
     * Requires the user to have been assigned as a Client to a project by a Builder,
     * or to already possess ROLE_CLIENT. Suspended accounts are rejected.
     */
    @Transactional
    public UserProfileResponse getClientProfile() {
        UUID userId = SecurityUtil.getCurrentUserId();
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for client: " + userId));

        // 1. Check suspended status
        if (profile.getStatus() == AccountStatus.SUSPENDED) {
            throw new ForbiddenException("Client account is suspended.");
        }

        // 2. Prevent role escalation: Builders and Contractors cannot obtain Client access
        if (userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER) ||
            userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CONTRACTOR)) {
            log.warn("Security violation: User {} with existing Builder/Contractor role attempted client access", userId);
            throw new ForbiddenException("Users registered as Builder or Contractor cannot access the Client portal or obtain a Client role.");
        }

        // 3. Verify Client eligibility and authorization (active project assignment or existing role)
        boolean hasClientRole = userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CLIENT);
        boolean isAssignedClient = projectMemberRepository.existsByUser_IdAndRoleAndStatus(
                userId, ProjectMemberRole.CLIENT, MemberStatus.ACTIVE);

        if (!hasClientRole && !isAssignedClient) {
            log.warn("Security rejection: User {} is not assigned to any project as a Client", userId);
            throw new ForbiddenException("Access denied: You are not authorized or assigned to any project as a Client.");
        }

        // 4. Activate profile if currently pending activation
        if (profile.getStatus() == AccountStatus.PENDING_ACTIVATION) {
            profile.setStatus(AccountStatus.ACTIVE);
            profile = profileRepository.save(profile);
        }

        // 5. Ensure ROLE_CLIENT is assigned (idempotent - no duplicate roles)
        if (!hasClientRole) {
            Role clientRole = roleRepository.findByName(RoleType.ROLE_CLIENT)
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .name(RoleType.ROLE_CLIENT)
                            .description("Client with read-only access to assigned projects")
                            .build()));

            UserRole userRole = UserRole.builder()
                    .user(profile)
                    .role(clientRole)
                    .assignedAt(LocalDateTime.now())
                    .build();
            userRoleRepository.save(userRole);
        }

        return getProfileResponse(profile);
    }

    private UserProfileResponse getProfileResponse(Profile profile) {
        List<String> roles = userRoleRepository.findByUser_Id(profile.getId())
                .stream()
                .map(ur -> ur.getRole().getName().name())
                .toList();

        return UserProfileResponse.builder()
                .id(profile.getId())
                .email(profile.getEmail())
                .fullName(profile.getFullName())
                .phoneNumber(profile.getPhoneNumber())
                .status(profile.getStatus())
                .avatarUrl(profile.getAvatarUrl())
                .roles(roles)
                .createdAt(profile.getCreatedAt())
                .build();
    }

    private String hashOtp(String otp, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
