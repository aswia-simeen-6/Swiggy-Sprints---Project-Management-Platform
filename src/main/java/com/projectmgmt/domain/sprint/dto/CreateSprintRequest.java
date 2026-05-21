package com.projectmgmt.domain.sprint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateSprintRequest(
        @NotBlank(message = "Sprint name is required")
        @Size(min = 1, max = 200, message = "Sprint name must be between 1 and 200 characters")
        String name,

        String goal,

        LocalDate startDate,

        LocalDate endDate
) {}
