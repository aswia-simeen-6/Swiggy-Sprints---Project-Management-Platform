package com.projectmgmt.event;

import com.projectmgmt.domain.issue.Issue;
import lombok.Getter;

import java.util.UUID;

@Getter
public class IssueCreatedEvent extends DomainEvent {
    private final Issue issue;
    private final UUID projectId;

    public IssueCreatedEvent(Issue issue, UUID projectId, UUID triggeredBy) {
        super(triggeredBy);
        this.issue = issue;
        this.projectId = projectId;
    }
}
