package com.projectmgmt.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projectmgmt.domain.activity.ActivityLog;
import com.projectmgmt.domain.activity.ActivityRepository;
import com.projectmgmt.domain.activity.ActivityType;
import com.projectmgmt.domain.issue.IssueWatcherRepository;
import com.projectmgmt.domain.notification.Notification;
import com.projectmgmt.domain.notification.NotificationRepository;
import com.projectmgmt.domain.notification.NotificationType;
import com.projectmgmt.domain.project.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventListener {

    private final ActivityRepository activityRepository;
    private final NotificationRepository notificationRepository;
    private final IssueWatcherRepository watcherRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ObjectMapper objectMapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueCreated(IssueCreatedEvent event) {
        log.debug("Processing IssueCreatedEvent for issue {}", event.getIssue().getIssueKey());

        activityRepository.save(ActivityLog.builder()
                .projectId(event.getProjectId())
                .issueId(event.getIssue().getId())
                .userId(event.getTriggeredBy())
                .action(ActivityType.ISSUE_CREATED)
                .changes(buildNode(Map.of(
                        "issueKey", event.getIssue().getIssueKey(),
                        "title", event.getIssue().getTitle(),
                        "type", event.getIssue().getIssueType().name())))
                .build());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueUpdated(IssueUpdatedEvent event) {
        log.debug("Processing IssueUpdatedEvent for issue {}", event.getIssue().getIssueKey());

        activityRepository.save(ActivityLog.builder()
                .projectId(event.getProjectId())
                .issueId(event.getIssue().getId())
                .userId(event.getTriggeredBy())
                .action(ActivityType.ISSUE_UPDATED)
                .changes(event.getChanges())
                .build());

        // Notify watchers
        Set<UUID> watchers = watcherRepository.findWatcherIdsByIssueId(event.getIssue().getId());
        watchers.remove(event.getTriggeredBy()); // Don't notify the person who made the change
        createNotifications(watchers, NotificationType.WATCHER_UPDATE,
                "Issue Updated: " + event.getIssue().getIssueKey(),
                event.getIssue().getTitle() + " was updated",
                "ISSUE", event.getIssue().getId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueTransitioned(IssueTransitionedEvent event) {
        log.debug("Processing IssueTransitionedEvent: {} → {}", event.getFromStatus(), event.getToStatus());

        activityRepository.save(ActivityLog.builder()
                .projectId(event.getProjectId())
                .issueId(event.getIssue().getId())
                .userId(event.getTriggeredBy())
                .action(ActivityType.ISSUE_TRANSITIONED)
                .changes(buildNode(Map.of(
                        "field", "status",
                        "old", event.getFromStatus(),
                        "new", event.getToStatus())))
                .build());

        // Notify watchers
        Set<UUID> watchers = watcherRepository.findWatcherIdsByIssueId(event.getIssue().getId());
        watchers.remove(event.getTriggeredBy());
        createNotifications(watchers, NotificationType.STATUS_CHANGED,
                event.getIssue().getIssueKey() + " → " + event.getToStatus(),
                "Status changed from " + event.getFromStatus() + " to " + event.getToStatus(),
                "ISSUE", event.getIssue().getId());

        // Notify assignee if different from trigger
        UUID assigneeId = event.getIssue().getAssigneeId();
        if (assigneeId != null && !assigneeId.equals(event.getTriggeredBy())) {
            createNotifications(Set.of(assigneeId), NotificationType.STATUS_CHANGED,
                    event.getIssue().getIssueKey() + " → " + event.getToStatus(),
                    "Your assigned issue was moved to " + event.getToStatus(),
                    "ISSUE", event.getIssue().getId());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentAdded(CommentAddedEvent event) {
        log.debug("Processing CommentAddedEvent on issue {}", event.getIssueId());

        activityRepository.save(ActivityLog.builder()
                .projectId(event.getProjectId())
                .issueId(event.getIssueId())
                .userId(event.getTriggeredBy())
                .action(ActivityType.COMMENT_ADDED)
                .changes(buildNode(Map.of(
                        "commentId", event.getComment().getId().toString(),
                        "bodyPreview", truncate(event.getComment().getBody(), 200))))
                .build());

        // Notify mentioned users
        UUID[] mentions = event.getComment().getMentions();
        if (mentions != null && mentions.length > 0) {
            Set<UUID> mentionedUsers = new HashSet<>(Arrays.asList(mentions));
            mentionedUsers.remove(event.getTriggeredBy());
            createNotifications(mentionedUsers, NotificationType.MENTIONED,
                    "You were mentioned in " + event.getIssueId(),
                    truncate(event.getComment().getBody(), 200),
                    "ISSUE", event.getIssueId());
        }

        // Notify watchers
        Set<UUID> watchers = watcherRepository.findWatcherIdsByIssueId(event.getIssueId());
        watchers.remove(event.getTriggeredBy());
        createNotifications(watchers, NotificationType.COMMENTED,
                "New comment on issue",
                truncate(event.getComment().getBody(), 200),
                "ISSUE", event.getIssueId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSprintUpdated(SprintUpdatedEvent event) {
        log.debug("Processing SprintUpdatedEvent: {} - {}", event.getSprint().getName(), event.getAction());

        ActivityType activityType = switch (event.getAction()) {
            case "CREATED" -> ActivityType.SPRINT_CREATED;
            case "STARTED" -> ActivityType.SPRINT_STARTED;
            case "COMPLETED" -> ActivityType.SPRINT_COMPLETED;
            default -> ActivityType.SPRINT_UPDATED;
        };

        activityRepository.save(ActivityLog.builder()
                .projectId(event.getProjectId())
                .userId(event.getTriggeredBy())
                .action(activityType)
                .changes(buildNode(Map.of(
                        "sprintId", event.getSprint().getId().toString(),
                        "sprintName", event.getSprint().getName(),
                        "action", event.getAction())))
                .build());

        // Notify all project members for sprint starts and completions
        if ("STARTED".equals(event.getAction()) || "COMPLETED".equals(event.getAction())) {
            Set<UUID> members = projectMemberRepository.findMemberIdsByProjectId(event.getProjectId());
            members.remove(event.getTriggeredBy());
            NotificationType notifType = "STARTED".equals(event.getAction())
                    ? NotificationType.SPRINT_STARTED : NotificationType.SPRINT_COMPLETED;
            createNotifications(members, notifType,
                    "Sprint " + event.getAction().toLowerCase() + ": " + event.getSprint().getName(),
                    event.getSprint().getGoal() != null ? event.getSprint().getGoal() : "",
                    "SPRINT", event.getSprint().getId());
        }
    }

    private void createNotifications(Set<UUID> recipientIds, NotificationType type,
                                      String title, String message, String resourceType, UUID resourceId) {
        if (recipientIds == null || recipientIds.isEmpty()) return;

        List<Notification> notifications = recipientIds.stream()
                .map(userId -> Notification.builder()
                        .userId(userId)
                        .type(type)
                        .title(title)
                        .message(message)
                        .resourceType(resourceType)
                        .resourceId(resourceId)
                        .build())
                .toList();
        notificationRepository.saveAll(notifications);
    }

    private ObjectNode buildNode(Map<String, String> fields) {
        ObjectNode node = objectMapper.createObjectNode();
        fields.forEach(node::put);
        return node;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
