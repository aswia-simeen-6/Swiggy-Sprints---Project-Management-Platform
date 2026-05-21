package com.projectmgmt.domain.issue.dto;

import java.util.List;
import java.util.UUID;

public record BoardResponse(
        UUID projectId,
        String projectName,
        String projectKey,
        List<BoardColumn> columns,
        UUID activeSprintId,
        String activeSprintName
) {
    public record BoardColumn(
            UUID statusId,
            String name,
            String category,
            String color,
            int position,
            List<IssueResponse> issues,
            int issueCount,
            int totalStoryPoints
    ) {}
}
