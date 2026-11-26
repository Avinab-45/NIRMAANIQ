package com.construction.controller;

import com.construction.dto.auth.*;
import com.construction.dto.common.ApiResponse;
import com.construction.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Builder activation endpoint.
     * Validates Supabase authenticated JWT, verifies invitation/approval code,
     * assigns ROLE_BUILDER, and sets account to ACTIVE.
     */
    @PostMapping("/builder/activate")
    public ResponseEntity<ApiResponse<UserProfileResponse>> activateBuilder(
            @Valid @RequestBody BuilderActivationRequest request) {
        UserProfileResponse response = authService.activateBuilder(request);
        return ResponseEntity.ok(ApiResponse.success("Builder account activated successfully.", response));
    }

    /**
     * Get builder profile for authenticated builder.
     */
    @GetMapping("/builder/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getBuilderProfile() {
        UserProfileResponse response = authService.getBuilderProfile();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Contractor requests a 6-digit email OTP for secondary verification.
     * Requires authenticated Supabase contractor JWT.
     * Enforces rate limiting, 5-minute expiry, max 5 attempts.
     */
    @PostMapping("/contractor/request-otp")
    public ResponseEntity<ApiResponse<Void>> requestContractorOtp(
            @Valid @RequestBody(required = false) ContractorOtpRequest request) {
        authService.requestContractorOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Verification OTP has been sent to your registered email.", null));
    }

    /**
     * Contractor verifies the 6-digit email OTP.
     * Requires authenticated Supabase contractor JWT.
     * One-time use, secure hash validation, assigns ROLE_CONTRACTOR and activates account.
     */
    @PostMapping("/contractor/verify-otp")
    public ResponseEntity<ApiResponse<UserProfileResponse>> verifyContractorOtp(
            @Valid @RequestBody ContractorVerifyOtpRequest request) {
        UserProfileResponse response = authService.verifyContractorOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Contractor verified successfully.", response));
    }

    /**
     * Get client profile for authenticated client using Supabase Email OTP.
     * Validates CLIENT role and account status.
     */
    @GetMapping("/client/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getClientProfile() {
        UserProfileResponse response = authService.getClientProfile();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
