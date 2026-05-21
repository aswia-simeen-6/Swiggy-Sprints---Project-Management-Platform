package com.projectmgmt.domain.project.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectMemberResponse(
        UUID id,
        UUID userId,
        String role,
        Instant createdAt
) {}
