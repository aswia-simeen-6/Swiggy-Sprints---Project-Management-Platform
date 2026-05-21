package com.projectmgmt.domain.user.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthResponse(
        String token,
        String tokenType,
        UserResponse user
) {
    public static AuthResponse of(String token, UserResponse user) {
        return new AuthResponse(token, "Bearer", user);
    }
}
