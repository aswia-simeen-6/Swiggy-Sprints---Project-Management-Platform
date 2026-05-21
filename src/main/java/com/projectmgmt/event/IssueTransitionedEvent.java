package com.projectmgmt.event;

import com.projectmgmt.domain.issue.Issue;
import lombok.Getter;

import java.util.UUID;

@Getter
public class IssueTransitionedEvent extends DomainEvent {
    private final Issue issue;
    private final UUID projectId;
    private final String fromStatus;
    private final String toStatus;
    private final UUID transitionId;

    public IssueTransitionedEvent(Issue issue, UUID projectId, String fromStatus, String toStatus,
                                   UUID transitionId, UUID triggeredBy) {
        super(triggeredBy);
        this.issue = issue;
        this.projectId = projectId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.transitionId = transitionId;
    }
}
