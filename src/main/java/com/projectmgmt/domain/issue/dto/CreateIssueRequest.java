package com.projectmgmt.domain.issue.dto;

import com.projectmgmt.domain.issue.IssueType;
import com.projectmgmt.domain.issue.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateIssueRequest(
        @NotNull(message = "Issue type is required")
        IssueType issueType,

        @NotBlank(message = "Title is required")
        @Size(min = 1, max = 500, message = "Title must be between 1 and 500 characters")
        String title,

        String description,

        Priority priority,

        UUID assigneeId,

        UUID sprintId,

        UUID parentId,

        Integer storyPoints,

        List<String> labels,

        Map<String, Object> customFields
) {}
