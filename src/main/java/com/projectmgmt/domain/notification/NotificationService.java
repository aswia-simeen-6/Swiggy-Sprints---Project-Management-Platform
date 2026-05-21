package com.projectmgmt.domain.notification;

import com.projectmgmt.domain.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(UUID userId, boolean unreadOnly, int page, int size) {
        Slice<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationRepository.findUnreadByUserId(userId, PageRequest.of(page, size));
        } else {
            notifications = notificationRepository.findByUserId(userId, PageRequest.of(page, size));
        }
        return notifications.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        notificationRepository.markAsRead(notificationId, userId);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getResourceType(), n.getResourceId(), n.isRead(), n.getCreatedAt());
    }
}
