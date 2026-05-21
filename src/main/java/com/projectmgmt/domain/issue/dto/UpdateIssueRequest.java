package com.projectmgmt.domain.issue.dto;

import com.projectmgmt.domain.issue.Priority;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record UpdateIssueRequest(
        String title,
        String description,
        Priority priority,
        UUID assigneeId,
        UUID sprintId,
        Integer storyPoints,
        List<String> labels,
        Map<String, Object> customFields,
        Integer version  // Required for optimistic locking
) {}
