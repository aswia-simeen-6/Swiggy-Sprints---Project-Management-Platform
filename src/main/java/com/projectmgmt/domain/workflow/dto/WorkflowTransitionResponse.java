package com.projectmgmt.domain.workflow.dto;

import java.util.List;
import java.util.UUID;

public record WorkflowTransitionResponse(
        UUID id,
        String name,
        UUID fromStatusId,
        String fromStatusName,
        UUID toStatusId,
        String toStatusName,
        List<ConditionResponse> conditions,
        List<ActionResponse> actions
) {
    public record ConditionResponse(UUID id, String conditionType, Object config) {}
    public record ActionResponse(UUID id, String actionType, Object config) {}
}
