package com.projectmgmt.domain.activity.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectmgmt.domain.activity.ActivityType;

import java.time.Instant;
import java.util.UUID;

public record ActivityResponse(
        UUID id,
        UUID projectId,
        UUID issueId,
        UUID userId,
        String userName,
        ActivityType action,
        JsonNode changes,
        JsonNode metadata,
        Instant createdAt
) {}
