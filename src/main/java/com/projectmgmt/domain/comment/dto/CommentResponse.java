package com.projectmgmt.domain.comment.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID issueId,
        UUID authorId,
        String authorName,
        UUID parentId,
        String body,
        List<UUID> mentions,
        List<CommentResponse> replies,
        Instant createdAt,
        Instant updatedAt,
        Integer version
) {}
