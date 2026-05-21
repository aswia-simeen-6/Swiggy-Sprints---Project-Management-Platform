package com.projectmgmt.domain.notification.dto;

import com.projectmgmt.domain.notification.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        String resourceType,
        UUID resourceId,
        boolean read,
        Instant createdAt
) {}
