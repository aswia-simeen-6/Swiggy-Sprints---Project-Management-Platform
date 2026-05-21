package com.projectmgmt.domain.sprint.dto;

import java.util.UUID;

public record SprintCompletionResult(
        SprintResponse sprint,
        int completedIssueCount,
        int incompleteIssueCount,
        int carriedOverCount,
        int velocity
) {}
