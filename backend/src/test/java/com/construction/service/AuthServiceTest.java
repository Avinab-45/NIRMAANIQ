package com.construction.service;

import com.construction.config.BuilderProperties;
import com.construction.dto.auth.BuilderActivationRequest;
import com.construction.dto.auth.ContractorOtpRequest;
import com.construction.dto.auth.ContractorVerifyOtpRequest;
import com.construction.dto.auth.UserProfileResponse;
import com.construction.entity.OtpChallenge;
import com.construction.entity.Profile;
import com.construction.entity.Role;
import com.construction.entity.UserRole;
import com.construction.entity.enums.AccountStatus;
import com.construction.entity.enums.MemberStatus;
import com.construction.entity.enums.ProjectMemberRole;
import com.construction.entity.enums.RoleType;
import com.construction.exception.BadRequestException;
import com.construction.exception.ForbiddenException;
import com.construction.exception.RateLimitException;
import com.construction.repository.OtpChallengeRepository;
import com.construction.repository.ProfileRepository;
import com.construction.repository.ProjectMemberRepository;
import com.construction.repository.RoleRepository;
import com.construction.repository.UserRoleRepository;
import com.construction.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private OtpChallengeRepository otpChallengeRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private BuilderProperties builderProperties;

    @InjectMocks
    private AuthService authService;

    private UUID userId;
    private Profile mockProfile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockProfile = Profile.builder()
                .id(userId)
                .email("builder@test.com")
                .status(AccountStatus.PENDING_ACTIVATION)
                .build();

        ReflectionTestUtils.setField(authService, "otpExpiryMinutes", 5);
        ReflectionTestUtils.setField(authService, "otpMaxAttempts", 5);
        ReflectionTestUtils.setField(authService, "otpResendIntervalSeconds", 60);

        setAuthenticatedUser(userId, "builder@test.com", AccountStatus.PENDING_ACTIVATION, "ROLE_BUILDER");
    }

    private void setAuthenticatedUser(UUID id, String email, AccountStatus status, String... roles) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();

        UserPrincipal principal = UserPrincipal.builder()
                .id(id)
                .email(email)
                .status(status)
                .authorities(authorities)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // BUILDER ACTIVATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Builder activation with valid secret activates account and assigns ROLE_BUILDER")
    void activateBuilder_ValidCode_Success() {
        BuilderActivationRequest request = BuilderActivationRequest.builder()
                .activationCode("VALID-SECRET-123")
                .build();

        when(builderProperties.getActivationCode()).thenReturn("VALID-SECRET-123");
        when(profileRepository.findById(userId)).thenReturn(Optional.of(mockProfile));
        when(profileRepository.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        Role builderRole = Role.builder().id(UUID.randomUUID()).name(RoleType.ROLE_BUILDER).build();
        when(roleRepository.findByName(RoleType.ROLE_BUILDER)).thenReturn(Optional.of(builderRole));
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)).thenReturn(false);

        UserRole savedUserRole = UserRole.builder().user(mockProfile).role(builderRole).build();
        when(userRoleRepository.findByUser_Id(userId)).thenReturn(List.of(savedUserRole));

        UserProfileResponse response = authService.activateBuilder(request);

        assertNotNull(response);
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
        assertTrue(response.getRoles().contains("ROLE_BUILDER"));
        verify(profileRepository).save(mockProfile);
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    @DisplayName("Builder activation with invalid code throws ForbiddenException")
    void activateBuilder_InvalidCode_ThrowsForbidden() {
        BuilderActivationRequest request = BuilderActivationRequest.builder()
                .activationCode("WRONG-CODE")
                .build();

        when(builderProperties.getActivationCode()).thenReturn("VALID-SECRET-123");

        assertThrows(ForbiddenException.class, () -> authService.activateBuilder(request));
        verify(profileRepository, never()).save(any());
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Builder activation with suspended account throws ForbiddenException")
    void activateBuilder_SuspendedAccount_ThrowsForbidden() {
        mockProfile.setStatus(AccountStatus.SUSPENDED);
        BuilderActivationRequest request = BuilderActivationRequest.builder()
                .activationCode("VALID-SECRET-123")
                .build();

        when(builderProperties.getActivationCode()).thenReturn("VALID-SECRET-123");
        when(profileRepository.findById(userId)).thenReturn(Optional.of(mockProfile));

        assertThrows(ForbiddenException.class, () -> authService.activateBuilder(request));
    }

    // =========================================================================
    // CONTRACTOR ROLE ESCALATION & OTP TESTS
    // =========================================================================

    @Test
    @DisplayName("Active invited Contractor can request OTP")
    void requestContractorOtp_ActiveInvitedContractor_Success() {
        String email = "contractor@test.com";
        setAuthenticatedUser(userId, email, AccountStatus.PENDING_ACTIVATION);

        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CLIENT)).thenReturn(false);
        when(projectMemberRepository.existsByUser_IdAndRoleAndStatus(userId, ProjectMemberRole.CONTRACTOR, MemberStatus.ACTIVE))
                .thenReturn(true);
        when(otpChallengeRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(eq(email), eq("CONTRACTOR_LOGIN")))
                .thenReturn(Optional.empty());
        when(otpChallengeRepository.save(any(OtpChallenge.class))).thenAnswer(i -> i.getArgument(0));

        ContractorOtpRequest request = ContractorOtpRequest.builder().email(email).build();

        assertDoesNotThrow(() -> authService.requestContractorOtp(request));
        verify(otpChallengeRepository).save(any(OtpChallenge.class));
        verify(emailService).sendOtpEmail(eq(email), anyString());
    }

    @Test
    @DisplayName("Inactive contractor invitation rejects OTP request with ForbiddenException")
    void requestContractorOtp_InactiveInvitation_ThrowsForbidden() {
        String email = "contractor@test.com";
        setAuthenticatedUser(userId, email, AccountStatus.PENDING_ACTIVATION);

        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CLIENT)).thenReturn(false);
        when(projectMemberRepository.existsByUser_IdAndRoleAndStatus(userId, ProjectMemberRole.CONTRACTOR, MemberStatus.ACTIVE))
                .thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CONTRACTOR)).thenReturn(false);

        ContractorOtpRequest request = ContractorOtpRequest.builder().email(email).build();

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> authService.requestContractorOtp(request));
        assertTrue(ex.getMessage().contains("active invitation or approval as a Contractor"));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Existing ROLE_CONTRACTOR cannot bypass suspended account check")
    void requestContractorOtp_ExistingRole_SuspendedAccount_ThrowsForbidden() {
        String email = "suspended.contractor@test.com";
        setAuthenticatedUser(userId, email, AccountStatus.SUSPENDED, "ROLE_CONTRACTOR");

        ContractorOtpRequest request = ContractorOtpRequest.builder().email(email).build();

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> authService.requestContractorOtp(request));
        assertTrue(ex.getMessage().contains("suspended"));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Arbitrary uninvited user requesting Contractor OTP throws ForbiddenException")
    void requestContractorOtp_UninvitedUser_ThrowsForbidden() {
        String email = "arbitrary@test.com";
        setAuthenticatedUser(userId, email, AccountStatus.PENDING_ACTIVATION);

        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CLIENT)).thenReturn(false);
        when(projectMemberRepository.existsByUser_IdAndRoleAndStatus(userId, ProjectMemberRole.CONTRACTOR, MemberStatus.ACTIVE))
                .thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CONTRACTOR)).thenReturn(false);

        ContractorOtpRequest request = ContractorOtpRequest.builder().email(email).build();

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> authService.requestContractorOtp(request));
        assertTrue(ex.getMessage().contains("active invitation or approval as a Contractor"));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Builder attempting to request Contractor OTP throws ForbiddenException")
    void requestContractorOtp_BuilderUser_ThrowsForbidden() {
        String email = "builder@test.com";
        setAuthenticatedUser(userId, email, AccountStatus.ACTIVE, "ROLE_BUILDER");

        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)).thenReturn(true);

        ContractorOtpRequest request = ContractorOtpRequest.builder().email(email).build();

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> authService.requestContractorOtp(request));
        assertTrue(ex.getMessage().contains("Users registered as Builder or Client cannot activate as a Contractor"));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Client attempting to request Contractor OTP throws ForbiddenException")
    void requestContractorOtp_ClientUser_ThrowsForbidden() {
        String email = "client@test.com";
        setAuthenticatedUser(userId, email, AccountStatus.ACTIVE, "ROLE_CLIENT");

        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CLIENT)).thenReturn(true);

        ContractorOtpRequest request = ContractorOtpRequest.builder().email(email).build();

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> authService.requestContractorOtp(request));
        assertTrue(ex.getMessage().contains("Users registered as Builder or Client cannot activate as a Contractor"));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Requesting Contractor OTP with mismatched email throws ForbiddenException")
    void requestContractorOtp_MismatchedEmail_ThrowsForbidden() {
        String authEmail = "auth@test.com";
        String differentEmail = "victim@test.com";
        setAuthenticatedUser(userId, authEmail, AccountStatus.PENDING_ACTIVATION);

        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CLIENT)).thenReturn(false);
        when(projectMemberRepository.existsByUser_IdAndRoleAndStatus(userId, ProjectMemberRole.CONTRACTOR, MemberStatus.ACTIVE))
                .thenReturn(true);

        ContractorOtpRequest request = ContractorOtpRequest.builder().email(differentEmail).build();

        assertThrows(ForbiddenException.class, () -> authService.requestContractorOtp(request));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Contractor OTP request within cooldown throws RateLimitException")
    void requestContractorOtp_CooldownActive_ThrowsRateLimit() {
        String email = "contractor@test.com";
        setAuthenticatedUser(userId, email, AccountStatus.PENDING_ACTIVATION);

        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CLIENT)).thenReturn(false);
        when(projectMemberRepository.existsByUser_IdAndRoleAndStatus(userId, ProjectMemberRole.CONTRACTOR, MemberStatus.ACTIVE))
                .thenReturn(true);

        OtpChallenge recentChallenge = OtpChallenge.builder()
                .id(UUID.randomUUID())
                .email(email)
                .createdAt(LocalDateTime.now().minusSeconds(20))
                .build();

        when(otpChallengeRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(eq(email), eq("CONTRACTOR_LOGIN")))
                .thenReturn(Optional.of(recentChallenge));

        ContractorOtpRequest request = ContractorOtpRequest.builder().email(email).build();

        assertThrows(RateLimitException.class, () -> authService.requestContractorOtp(request));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Contractor verify OTP with correct code activates account and assigns ROLE_CONTRACTOR")
    void verifyContractorOtp_ValidOtp_Success() {
        String email = "contractor@test.com";
        String otp = "654321";
        setAuthenticatedUser(userId, email, AccountStatus.PENDING_ACTIVATION);

        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CLIENT)).thenReturn(false);
        when(projectMemberRepository.existsByUser_IdAndRoleAndStatus(userId, ProjectMemberRole.CONTRACTOR, MemberStatus.ACTIVE))
                .thenReturn(true);

        String salt = email.toLowerCase().trim();
        String expectedHash;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            expectedHash = HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        OtpChallenge challenge = OtpChallenge.builder()
                .id(UUID.randomUUID())
                .email(email)
                .otpHash(expectedHash)
                .purpose("CONTRACTOR_LOGIN")
                .attemptsCount(0)
                .maxAttempts(5)
                .isUsed(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(otpChallengeRepository.findFirstByEmailAndPurposeAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(email), eq("CONTRACTOR_LOGIN"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(challenge));

        Profile contractorProfile = Profile.builder()
                .id(userId)
                .email(email)
                .status(AccountStatus.PENDING_ACTIVATION)
                .build();

        when(profileRepository.findById(userId)).thenReturn(Optional.of(contractorProfile));
        when(profileRepository.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        Role contractorRole = Role.builder().id(UUID.randomUUID()).name(RoleType.ROLE_CONTRACTOR).build();
        when(roleRepository.findByName(RoleType.ROLE_CONTRACTOR)).thenReturn(Optional.of(contractorRole));
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CONTRACTOR)).thenReturn(false);

        UserRole userRole = UserRole.builder().user(contractorProfile).role(contractorRole).build();
        when(userRoleRepository.findByUser_Id(userId)).thenReturn(List.of(userRole));

        ContractorVerifyOtpRequest verifyRequest = ContractorVerifyOtpRequest.builder()
                .email(email)
                .otp(otp)
                .build();

        UserProfileResponse response = authService.verifyContractorOtp(verifyRequest);

        assertNotNull(response);
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
        assertTrue(response.getRoles().contains("ROLE_CONTRACTOR"));
        assertTrue(challenge.isUsed());
        verify(otpChallengeRepository).save(challenge);
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    @DisplayName("Contractor verify OTP with inactive invitation throws ForbiddenException")
    void verifyContractorOtp_InactiveInvitation_ThrowsForbidden() {
        String email = "contractor@test.com";
        setAuthenticatedUser(userId, email, AccountStatus.PENDING_ACTIVATION);

        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CLIENT)).thenReturn(false);
        when(projectMemberRepository.existsByUser_IdAndRoleAndStatus(userId, ProjectMemberRole.CONTRACTOR, MemberStatus.ACTIVE))
                .thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CONTRACTOR)).thenReturn(false);

        ContractorVerifyOtpRequest verifyRequest = ContractorVerifyOtpRequest.builder()
                .email(email)
                .otp("123456")
                .build();

        assertThrows(ForbiddenException.class, () -> authService.verifyContractorOtp(verifyRequest));
        verify(otpChallengeRepository, never()).findFirstByEmailAndPurposeAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    @DisplayName("Contractor OTP verify with uninvited user throws ForbiddenException")
    void verifyContractorOtp_UninvitedUser_ThrowsForbidden() {
        String email = "uninvited@test.com";
        setAuthenticatedUser(userId, email, AccountStatus.PENDING_ACTIVATION);

        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CLIENT)).thenReturn(false);
        when(projectMemberRepository.existsByUser_IdAndRoleAndStatus(userId, ProjectMemberRole.CONTRACTOR, MemberStatus.ACTIVE))
                .thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CONTRACTOR)).thenReturn(false);

        ContractorVerifyOtpRequest verifyRequest = ContractorVerifyOtpRequest.builder()
                .email(email)
                .otp("123456")
                .build();

        assertThrows(ForbiddenException.class, () -> authService.verifyContractorOtp(verifyRequest));
        verify(otpChallengeRepository, never()).findFirstByEmailAndPurposeAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    @DisplayName("Contractor OTP verify with incorrect code increments attempts")
    void verifyContractorOtp_IncorrectOtp_IncrementsAttempts() {
        String email = "contractor@test.com";
        setAuthenticatedUser(userId, email, AccountStatus.PENDING_ACTIVATION);

        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CLIENT)).thenReturn(false);
        when(projectMemberRepository.existsByUser_IdAndRoleAndStatus(userId, ProjectMemberRole.CONTRACTOR, MemberStatus.ACTIVE))
                .thenReturn(true);

        OtpChallenge challenge = OtpChallenge.builder()
                .id(UUID.randomUUID())
                .email(email)
                .otpHash("validHashDifferentFromSupplied")
                .purpose("CONTRACTOR_LOGIN")
                .attemptsCount(1)
                .maxAttempts(5)
                .isUsed(false)
                .expiresAt(LocalDateTime.now().plusMinutes(4))
                .build();

        when(otpChallengeRepository.findFirstByEmailAndPurposeAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(email), eq("CONTRACTOR_LOGIN"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(challenge));

        ContractorVerifyOtpRequest verifyRequest = ContractorVerifyOtpRequest.builder()
                .email(email)
                .otp("000000")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.verifyContractorOtp(verifyRequest));
        assertTrue(ex.getMessage().contains("Invalid OTP"));
        assertEquals(2, challenge.getAttemptsCount());
        verify(otpChallengeRepository).save(challenge);
    }

    @Test
    @DisplayName("Contractor OTP verify exceeding max attempts throws BadRequestException and locks challenge")
    void verifyContractorOtp_MaxAttemptsExceeded_ThrowsBadRequest() {
        String email = "contractor@test.com";
        setAuthenticatedUser(userId, email, AccountStatus.PENDING_ACTIVATION);

        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CLIENT)).thenReturn(false);
        when(projectMemberRepository.existsByUser_IdAndRoleAndStatus(userId, ProjectMemberRole.CONTRACTOR, MemberStatus.ACTIVE))
                .thenReturn(true);

        OtpChallenge challenge = OtpChallenge.builder()
                .id(UUID.randomUUID())
                .email(email)
                .otpHash("dummyHash")
                .purpose("CONTRACTOR_LOGIN")
                .attemptsCount(5)
                .maxAttempts(5)
                .isUsed(false)
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .build();

        when(otpChallengeRepository.findFirstByEmailAndPurposeAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(email), eq("CONTRACTOR_LOGIN"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(challenge));

        ContractorVerifyOtpRequest verifyRequest = ContractorVerifyOtpRequest.builder()
                .email(email)
                .otp("999999")
                .build();

        assertThrows(BadRequestException.class, () -> authService.verifyContractorOtp(verifyRequest));
        assertTrue(challenge.isUsed());
        verify(otpChallengeRepository).save(challenge);
    }

    // =========================================================================
    // CLIENT ROLE ESCALATION & ONBOARDING TESTS
    // =========================================================================

    @Test
    @DisplayName("Eligible Client assigned to project completes onboarding via getClientProfile")
    void getClientProfile_EligibleClient_Success() {
        String email = "client@example.com";
        setAuthenticatedUser(userId, email, AccountStatus.PENDING_ACTIVATION);

        Profile clientProfile = Profile.builder()
                .id(userId)
                .email(email)
                .status(AccountStatus.PENDING_ACTIVATION)
                .build();

        when(profileRepository.findById(userId)).thenReturn(Optional.of(clientProfile));
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CONTRACTOR)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CLIENT)).thenReturn(false);
        when(projectMemberRepository.existsByUser_IdAndRoleAndStatus(userId, ProjectMemberRole.CLIENT, MemberStatus.ACTIVE))
                .thenReturn(true);

        when(profileRepository.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        Role clientRole = Role.builder().id(UUID.randomUUID()).name(RoleType.ROLE_CLIENT).build();
        when(roleRepository.findByName(RoleType.ROLE_CLIENT)).thenReturn(Optional.of(clientRole));

        UserRole ur = UserRole.builder().user(clientProfile).role(clientRole).build();
        when(userRoleRepository.findByUser_Id(userId)).thenReturn(List.of(ur));

        UserProfileResponse response = authService.getClientProfile();

        assertNotNull(response);
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
        assertTrue(response.getRoles().contains("ROLE_CLIENT"));
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    @DisplayName("Builder attempting to access Client profile throws ForbiddenException (prevents role escalation)")
    void getClientProfile_BuilderUser_ThrowsForbidden() {
        String email = "builder@test.com";
        setAuthenticatedUser(userId, email, AccountStatus.ACTIVE, "ROLE_BUILDER");

        Profile builderProfile = Profile.builder().id(userId).email(email).status(AccountStatus.ACTIVE).build();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(builderProfile));
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)).thenReturn(true);

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> authService.getClientProfile());
        assertTrue(ex.getMessage().contains("Users registered as Builder or Contractor cannot access the Client portal"));
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Contractor attempting to access Client profile throws ForbiddenException (prevents role escalation)")
    void getClientProfile_ContractorUser_ThrowsForbidden() {
        String email = "contractor@test.com";
        setAuthenticatedUser(userId, email, AccountStatus.ACTIVE, "ROLE_CONTRACTOR");

        Profile contractorProfile = Profile.builder().id(userId).email(email).status(AccountStatus.ACTIVE).build();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(contractorProfile));
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CONTRACTOR)).thenReturn(true);

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> authService.getClientProfile());
        assertTrue(ex.getMessage().contains("Users registered as Builder or Contractor cannot access the Client portal"));
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Suspended client attempting to access Client profile throws ForbiddenException")
    void getClientProfile_SuspendedClient_ThrowsForbidden() {
        String email = "suspended.client@test.com";
        setAuthenticatedUser(userId, email, AccountStatus.SUSPENDED);

        Profile suspendedProfile = Profile.builder().id(userId).email(email).status(AccountStatus.SUSPENDED).build();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(suspendedProfile));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> authService.getClientProfile());
        assertTrue(ex.getMessage().contains("suspended"));
    }

    @Test
    @DisplayName("Arbitrary uninvited user attempting to access Client profile throws ForbiddenException")
    void getClientProfile_UninvitedArbitraryUser_ThrowsForbidden() {
        String email = "stranger@test.com";
        setAuthenticatedUser(userId, email, AccountStatus.PENDING_ACTIVATION);

        Profile profile = Profile.builder().id(userId).email(email).status(AccountStatus.PENDING_ACTIVATION).build();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CONTRACTOR)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CLIENT)).thenReturn(false);
        when(projectMemberRepository.existsByUser_IdAndRoleAndStatus(userId, ProjectMemberRole.CLIENT, MemberStatus.ACTIVE))
                .thenReturn(false);

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> authService.getClientProfile());
        assertTrue(ex.getMessage().contains("not authorized or assigned to any project as a Client"));
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Repeated requests by existing Client does not create duplicate roles")
    void getClientProfile_RepeatedRequests_DoesNotDuplicateRole() {
        String email = "client@example.com";
        setAuthenticatedUser(userId, email, AccountStatus.ACTIVE, "ROLE_CLIENT");

        Profile clientProfile = Profile.builder().id(userId).email(email).status(AccountStatus.ACTIVE).build();

        when(profileRepository.findById(userId)).thenReturn(Optional.of(clientProfile));
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_BUILDER)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CONTRACTOR)).thenReturn(false);
        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleType.ROLE_CLIENT)).thenReturn(true);

        Role clientRole = Role.builder().name(RoleType.ROLE_CLIENT).build();
        UserRole ur = UserRole.builder().user(clientProfile).role(clientRole).build();
        when(userRoleRepository.findByUser_Id(userId)).thenReturn(List.of(ur));

        UserProfileResponse response = authService.getClientProfile();

        assertNotNull(response);
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
        verify(userRoleRepository, never()).save(any());
    }
}
