package com.projectmgmt.domain.workflow.dto;

import java.util.UUID;

public record WorkflowStatusResponse(
        UUID id,
        UUID projectId,
        String name,
        String category,
        int position,
        String color
) {}
