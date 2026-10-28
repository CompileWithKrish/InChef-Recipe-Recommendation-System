package com.inchef.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    public static Long getRequiredCurrentUserId() {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new InsufficientAuthenticationException("Authentication required: Please log in to access this resource");
        }
        return currentUserId;
    }

    public static void validateUserAccess(Long targetUserId) {
        Long currentUserId = getRequiredCurrentUserId();
        if (targetUserId != null && !currentUserId.equals(targetUserId)) {
            throw new AccessDeniedException("Forbidden: You cannot access or modify another user's data");
        }
    }
}

