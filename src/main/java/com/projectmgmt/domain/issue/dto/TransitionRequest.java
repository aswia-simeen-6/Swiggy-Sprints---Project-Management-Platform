package com.projectmgmt.domain.issue.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TransitionRequest(
        @NotNull(message = "Target status ID is required")
        UUID targetStatusId
) {}
