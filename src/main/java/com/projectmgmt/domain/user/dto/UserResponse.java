package com.projectmgmt.domain.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        String role,
        Instant createdAt
) {}
