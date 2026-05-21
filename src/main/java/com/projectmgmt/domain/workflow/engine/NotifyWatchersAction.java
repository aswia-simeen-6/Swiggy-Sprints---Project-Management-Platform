package com.projectmgmt.domain.workflow.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectmgmt.domain.issue.Issue;
import com.projectmgmt.domain.issue.IssueWatcherRepository;
import com.projectmgmt.domain.notification.Notification;
import com.projectmgmt.domain.notification.NotificationRepository;
import com.projectmgmt.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyWatchersAction implements TransitionActionExecutor {

    private final IssueWatcherRepository watcherRepository;
    private final NotificationRepository notificationRepository;

    @Override
    public String getType() {
        return "NOTIFY_WATCHERS";
    }

    @Override
    public void execute(Issue issue, JsonNode config, UUID triggeredBy) {
        String message = config.path("message").asText("Issue status was updated");

        Set<UUID> watchers = watcherRepository.findWatcherIdsByIssueId(issue.getId());
        watchers.remove(triggeredBy);

        if (watchers.isEmpty()) return;

        List<Notification> notifications = watchers.stream()
                .map(userId -> Notification.builder()
                        .userId(userId)
                        .type(NotificationType.STATUS_CHANGED)
                        .title(issue.getIssueKey() + ": " + message)
                        .message(message)
                        .resourceType("ISSUE")
                        .resourceId(issue.getId())
                        .build())
                .toList();

        notificationRepository.saveAll(notifications);
        log.debug("Sent {} notifications for action NOTIFY_WATCHERS on issue {}", notifications.size(), issue.getIssueKey());
    }
}
