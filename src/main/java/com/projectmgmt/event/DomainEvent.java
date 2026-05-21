package com.projectmgmt.event;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public abstract class DomainEvent {
    private final UUID eventId = UUID.randomUUID();
    private final Instant occurredAt = Instant.now();
    private final UUID triggeredBy;

    protected DomainEvent(UUID triggeredBy) {
        this.triggeredBy = triggeredBy;
    }
}
