package com.projectmgmt.domain.sprint.dto;

import com.projectmgmt.domain.sprint.SprintStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SprintResponse(
        UUID id,
        UUID projectId,
        String name,
        String goal,
        SprintStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Instant startedAt,
        Instant completedAt,
        Integer velocity,
        Integer completedPoints,
        Integer totalPoints,
        Instant createdAt,
        Instant updatedAt,
        Integer version
) {}
