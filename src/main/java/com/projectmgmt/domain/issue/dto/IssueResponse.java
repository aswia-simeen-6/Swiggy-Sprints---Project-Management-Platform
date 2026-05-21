package com.projectmgmt.domain.issue.dto;

import com.projectmgmt.domain.issue.IssueType;
import com.projectmgmt.domain.issue.Priority;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record IssueResponse(
        UUID id,
        UUID projectId,
        String issueKey,
        IssueType issueType,
        String title,
        String description,
        UUID statusId,
        String statusName,
        String statusCategory,
        Priority priority,
        UUID assigneeId,
        String assigneeName,
        UUID reporterId,
        UUID sprintId,
        UUID parentId,
        Integer storyPoints,
        List<String> labels,
        Map<String, Object> customFields,
        Instant createdAt,
        Instant updatedAt,
        Integer version
) {}
