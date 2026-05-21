package com.projectmgmt.domain.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tracks which users are online and what board they're currently viewing.
 * Uses Redis Sets for distributed state across multiple app instances.
 *
 * Keys:
 *   presence:online         → SET of userId strings (global online users)
 *   presence:board:{id}     → SET of userId strings (users viewing a specific board)
 *   presence:user:{id}      → STRING boardId (which board a user is on, with TTL)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final StringRedisTemplate redis;

    private static final String ONLINE_KEY = "presence:online";
    private static final String BOARD_KEY_PREFIX = "presence:board:";
    private static final String USER_KEY_PREFIX = "presence:user:";
    private static final Duration PRESENCE_TTL = Duration.ofMinutes(5);

    public void userConnected(UUID userId) {
        redis.opsForSet().add(ONLINE_KEY, userId.toString());
        log.debug("User connected: {}", userId);
    }

    public void userDisconnected(UUID userId) {
        // Remove from online set
        redis.opsForSet().remove(ONLINE_KEY, userId.toString());

        // Remove from any board they were viewing
        String currentBoard = redis.opsForValue().get(USER_KEY_PREFIX + userId);
        if (currentBoard != null) {
            redis.opsForSet().remove(BOARD_KEY_PREFIX + currentBoard, userId.toString());
            redis.delete(USER_KEY_PREFIX + userId);
        }
        log.debug("User disconnected: {}", userId);
    }

    public void userViewingBoard(UUID userId, UUID boardId) {
        String userKey = USER_KEY_PREFIX + userId;

        // Leave previous board if any
        String previousBoard = redis.opsForValue().get(userKey);
        if (previousBoard != null && !previousBoard.equals(boardId.toString())) {
            redis.opsForSet().remove(BOARD_KEY_PREFIX + previousBoard, userId.toString());
        }

        // Join new board
        redis.opsForSet().add(BOARD_KEY_PREFIX + boardId, userId.toString());
        redis.opsForValue().set(userKey, boardId.toString(), PRESENCE_TTL);
        log.debug("User {} viewing board {}", userId, boardId);
    }

    public void userLeftBoard(UUID userId, UUID boardId) {
        redis.opsForSet().remove(BOARD_KEY_PREFIX + boardId, userId.toString());
        redis.delete(USER_KEY_PREFIX + userId);
    }

    public Set<UUID> getUsersOnBoard(UUID boardId) {
        Set<String> members = redis.opsForSet().members(BOARD_KEY_PREFIX + boardId);
        if (members == null) return Set.of();
        return members.stream().map(UUID::fromString).collect(Collectors.toSet());
    }

    public boolean isUserOnline(UUID userId) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember(ONLINE_KEY, userId.toString()));
    }

    public void refreshPresence(UUID userId) {
        String userKey = USER_KEY_PREFIX + userId;
        String currentBoard = redis.opsForValue().get(userKey);
        if (currentBoard != null) {
            redis.expire(userKey, PRESENCE_TTL);
        }
    }
}
