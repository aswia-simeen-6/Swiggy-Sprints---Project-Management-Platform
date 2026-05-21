package com.projectmgmt.domain.project.dto;

import java.util.UUID;

public record MemberDetailResponse(
        UUID userId,
        String displayName,
        String avatarUrl,
        String role
) {}