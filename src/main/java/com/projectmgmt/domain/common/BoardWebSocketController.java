package com.projectmgmt.domain.common;

import com.projectmgmt.domain.user.User;
import com.projectmgmt.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class BoardWebSocketController {

    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    /**
     * Client sends: /app/board/{projectId}/join
     * Triggers: presence update + broadcast to all board viewers
     */
    @MessageMapping("/board/{projectId}/join")
    public void joinBoard(@DestinationVariable UUID projectId, Principal principal) {
        UUID userId = extractUserId(principal);
        presenceService.userViewingBoard(userId, projectId);

        // Broadcast updated presence to everyone on this board
        broadcastPresence(projectId);
        log.debug("User {} joined board {}", userId, projectId);
    }

    /**
     * Client sends: /app/board/{projectId}/leave
     */
    @MessageMapping("/board/{projectId}/leave")
    public void leaveBoard(@DestinationVariable UUID projectId, Principal principal) {
        UUID userId = extractUserId(principal);
        presenceService.userLeftBoard(userId, projectId);
        broadcastPresence(projectId);
    }

    /**
     * Client sends: /app/board/{projectId}/heartbeat
     * Keeps presence alive (called every 30s from client)
     */
    @MessageMapping("/board/{projectId}/heartbeat")
    public void heartbeat(@DestinationVariable UUID projectId, Principal principal) {
        UUID userId = extractUserId(principal);
        presenceService.refreshPresence(userId);
    }

    /**
     * Client sends: /app/board/{projectId}/cursor
     * Broadcasts cursor position to other board viewers (for collaborative feel)
     */
    @MessageMapping("/board/{projectId}/cursor")
    public void cursorMove(@DestinationVariable UUID projectId,
                           @Payload CursorPosition position, Principal principal) {
        UUID userId = extractUserId(principal);
        position.setUserId(userId);
        messagingTemplate.convertAndSend("/topic/board/" + projectId + "/cursors", position);
    }

    private void broadcastPresence(UUID projectId) {
        Set<UUID> userIds = presenceService.getUsersOnBoard(projectId);
        List<PresenceInfo> presenceList = userRepository.findAllById(userIds).stream()
                .map(u -> new PresenceInfo(u.getId(), u.getDisplayName(), u.getAvatarUrl()))
                .toList();

        messagingTemplate.convertAndSend(
                "/topic/board/" + projectId + "/presence",
                Map.of("users", presenceList, "count", presenceList.size(), "timestamp", Instant.now()));
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            User user = (User) auth.getPrincipal();
            return user.getId();
        }
        throw new IllegalStateException("No authenticated user in WebSocket session");
    }

    // Inner DTOs for WebSocket messages
    public record PresenceInfo(UUID userId, String displayName, String avatarUrl) {}

    @lombok.Data
    public static class CursorPosition {
        private UUID userId;
        private String columnId;
        private double x;
        private double y;
    }
}
