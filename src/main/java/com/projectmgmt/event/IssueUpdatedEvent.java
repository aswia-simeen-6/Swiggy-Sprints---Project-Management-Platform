package com.projectmgmt.event;

import com.projectmgmt.domain.issue.Issue;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import java.util.UUID;

@Getter
public class IssueUpdatedEvent extends DomainEvent {
    private final Issue issue;
    private final UUID projectId;
    private final JsonNode changes;

    public IssueUpdatedEvent(Issue issue, UUID projectId, JsonNode changes, UUID triggeredBy) {
        super(triggeredBy);
        this.issue = issue;
        this.projectId = projectId;
        this.changes = changes;
    }
}
