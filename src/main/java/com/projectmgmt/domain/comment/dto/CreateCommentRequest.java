package com.projectmgmt.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCommentRequest(
        @NotBlank(message = "Comment body is required")
        @Size(min = 1, max = 50000, message = "Comment body must be between 1 and 50000 characters")
        String body,

        UUID parentId
) {}
