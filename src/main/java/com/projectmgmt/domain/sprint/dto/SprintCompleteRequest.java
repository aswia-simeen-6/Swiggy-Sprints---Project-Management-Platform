package com.projectmgmt.domain.sprint.dto;

import java.util.List;
import java.util.UUID;

public record SprintCompleteRequest(
        List<UUID> carryOverIssueIds,
        UUID targetSprintId
) {
    public SprintCompleteRequest {
        if (carryOverIssueIds == null) {
            carryOverIssueIds = List.of();
        }
    }
}
