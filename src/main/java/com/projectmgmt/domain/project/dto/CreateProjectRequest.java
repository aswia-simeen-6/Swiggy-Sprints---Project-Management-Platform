package com.projectmgmt.domain.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "Project name is required")
        @Size(min = 2, max = 200, message = "Project name must be between 2 and 200 characters")
        String name,

        @NotBlank(message = "Project key is required")
        @Size(min = 2, max = 10, message = "Project key must be between 2 and 10 characters")
        @Pattern(regexp = "^[A-Z][A-Z0-9]*$", message = "Project key must start with a letter and contain only uppercase letters and numbers")
        String key,

        String description
) {}
