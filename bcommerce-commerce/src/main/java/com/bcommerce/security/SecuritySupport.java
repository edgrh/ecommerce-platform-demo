package com.bcommerce.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

public final class SecuritySupport {

    private SecuritySupport() {}

    public static AuthenticatedUser requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in required");
        }
        return u;
    }

    public static void requireRole(AuthenticatedUser user, String role) {
        if (!role.equalsIgnoreCase(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role required: " + role);
        }
    }
}
