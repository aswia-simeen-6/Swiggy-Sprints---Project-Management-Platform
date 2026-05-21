package com.projectmgmt.common.exception;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class WorkflowViolationException extends BaseException {

    private final String currentStatus;
    private final String targetStatus;
    private final List<String> allowedTransitions;

    public WorkflowViolationException(String currentStatus, String targetStatus,
                                       List<String> allowedTransitions) {
        super("WORKFLOW_TRANSITION_NOT_ALLOWED",
                "Cannot transition from '%s' to '%s'".formatted(currentStatus, targetStatus),
                Map.of(
                        "currentStatus", currentStatus,
                        "targetStatus", targetStatus,
                        "allowedTransitions", allowedTransitions
                ));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
        this.allowedTransitions = allowedTransitions;
    }
}
