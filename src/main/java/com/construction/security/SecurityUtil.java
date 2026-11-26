package com.construction.security;

import com.construction.entity.enums.AccountStatus;
import com.construction.entity.enums.RoleType;
import com.construction.exception.ForbiddenException;
import com.construction.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtil {

    private SecurityUtil() {}

    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }

        throw new UnauthorizedException("Invalid authentication principal");
    }

    public static UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static String getCurrentUserEmail() {
        return getCurrentUser().getEmail();
    }

    public static void requireActiveAccount() {
        UserPrincipal user = getCurrentUser();
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenException("Account is not active. Current status: " + user.getStatus());
        }
    }

    public static boolean hasRole(RoleType role) {
        UserPrincipal user = getCurrentUser();
        if (user.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority authority : user.getAuthorities()) {
            if (authority.getAuthority().equals(role.name())) {
                return true;
            }
        }
        return false;
    }

    public static void requireRole(RoleType role) {
        requireActiveAccount();
        if (!hasRole(role)) {
            throw new ForbiddenException("Access denied: required role " + role.name());
        }
    }
}
