package com.projectmgmt.event;

import com.projectmgmt.domain.sprint.Sprint;
import lombok.Getter;

import java.util.UUID;

@Getter
public class SprintUpdatedEvent extends DomainEvent {
    private final Sprint sprint;
    private final UUID projectId;
    private final String action; // "STARTED", "COMPLETED", "CREATED", "UPDATED"

    public SprintUpdatedEvent(Sprint sprint, UUID projectId, String action, UUID triggeredBy) {
        super(triggeredBy);
        this.sprint = sprint;
        this.projectId = projectId;
        this.action = action;
    }
}
