package com.projectmgmt.domain.project.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String key,
        String description,
        UUID ownerId,
        int issueCounter,
        List<ProjectMemberResponse> members,
        Instant createdAt,
        Instant updatedAt,
        Integer version
) {}
