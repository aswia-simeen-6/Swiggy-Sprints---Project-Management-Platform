package com.projectmgmt.domain.common;

import com.projectmgmt.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Listens to domain events and broadcasts real-time updates to WebSocket subscribers.
 * Each board gets its own topic: /topic/board/{projectId}/updates
 * Each user gets personal notifications: /user/{userId}/queue/notifications
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BoardEventBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @TransactionalEventListener
    public void onIssueCreated(IssueCreatedEvent event) {
        broadcast(event.getProjectId(), "ISSUE_CREATED", Map.of(
                "issueId", event.getIssue().getId(),
                "issueKey", event.getIssue().getIssueKey(),
                "title", event.getIssue().getTitle(),
                "statusId", event.getIssue().getStatusId(),
                "priority", event.getIssue().getPriority().name(),
                "triggeredBy", event.getTriggeredBy()
        ));
    }

    @Async
    @TransactionalEventListener
    public void onIssueUpdated(IssueUpdatedEvent event) {
        broadcast(event.getProjectId(), "ISSUE_UPDATED", Map.of(
                "issueId", event.getIssue().getId(),
                "issueKey", event.getIssue().getIssueKey(),
                "changes", event.getChanges(),
                "triggeredBy", event.getTriggeredBy()
        ));
    }

    @Async
    @TransactionalEventListener
    public void onIssueTransitioned(IssueTransitionedEvent event) {
        broadcast(event.getProjectId(), "ISSUE_TRANSITIONED", Map.of(
                "issueId", event.getIssue().getId(),
                "issueKey", event.getIssue().getIssueKey(),
                "fromStatus", event.getFromStatus(),
                "toStatus", event.getToStatus(),
                "statusId", event.getIssue().getStatusId(),
                "triggeredBy", event.getTriggeredBy()
        ));
    }

    @Async
    @TransactionalEventListener
    public void onCommentAdded(CommentAddedEvent event) {
        broadcast(event.getProjectId(), "COMMENT_ADDED", Map.of(
                "issueId", event.getIssueId(),
                "commentId", event.getComment().getId(),
                "authorId", event.getComment().getAuthorId(),
                "body", truncate(event.getComment().getBody(), 100),
                "triggeredBy", event.getTriggeredBy()
        ));
    }

    @Async
    @TransactionalEventListener
    public void onSprintUpdated(SprintUpdatedEvent event) {
        broadcast(event.getProjectId(), "SPRINT_" + event.getAction(), Map.of(
                "sprintId", event.getSprint().getId(),
                "sprintName", event.getSprint().getName(),
                "status", event.getSprint().getStatus().name(),
                "triggeredBy", event.getTriggeredBy()
        ));
    }

    private void broadcast(UUID projectId, String type, Map<String, Object> payload) {
        var message = Map.of(
                "type", type,
                "projectId", projectId,
                "payload", payload,
                "timestamp", Instant.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/board/" + projectId + "/updates", message);
        log.debug("Broadcast {} to board {}", type, projectId);
    }

    /**
     * Send a personal notification to a specific user via their private queue.
     */
    public void notifyUser(UUID userId, String type, Map<String, Object> payload) {
        var message = Map.of("type", type, "payload", payload, "timestamp", Instant.now().toString());
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", message);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }
}
