package com.projectmgmt.domain.notification;

import com.projectmgmt.common.model.ApiResponse;
import com.projectmgmt.common.util.SecurityUtils;
import com.projectmgmt.domain.notification.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification management")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get notifications for the current user")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<NotificationResponse> response = notificationService.getNotifications(
                SecurityUtils.getCurrentUserId(), unreadOnly, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get count of unread notifications")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount() {
        long count = notificationService.getUnreadCount(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Notification marked as read"));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        notificationService.markAllAsRead(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "All notifications marked as read"));
    }
}
