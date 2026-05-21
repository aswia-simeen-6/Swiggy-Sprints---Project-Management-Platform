package com.projectmgmt.common.util;

import com.projectmgmt.common.exception.UnauthorizedException;
import com.projectmgmt.domain.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User)) {
            throw new UnauthorizedException("No authenticated user found");
        }
        return (User) auth.getPrincipal();
    }

    public static UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
}