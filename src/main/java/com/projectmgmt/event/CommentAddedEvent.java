package com.projectmgmt.event;

import com.projectmgmt.domain.comment.Comment;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CommentAddedEvent extends DomainEvent {
    private final Comment comment;
    private final UUID projectId;
    private final UUID issueId;

    public CommentAddedEvent(Comment comment, UUID projectId, UUID issueId, UUID triggeredBy) {
        super(triggeredBy);
        this.comment = comment;
        this.projectId = projectId;
        this.issueId = issueId;
    }
}
