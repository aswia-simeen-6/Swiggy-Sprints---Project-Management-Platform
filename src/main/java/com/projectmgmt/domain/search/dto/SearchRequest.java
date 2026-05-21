package com.projectmgmt.domain.search.dto;

import com.projectmgmt.domain.issue.Priority;

import java.util.List;
import java.util.UUID;

public record SearchRequest(
        String query,
        UUID statusId,
        UUID assigneeId,
        Priority priority,
        List<String> labels,
        UUID sprintId,
        String cursor,
        int limit
) {
    public SearchRequest {
        if (limit <= 0 || limit > 100) {
            limit = 20;
        }
    }
}
