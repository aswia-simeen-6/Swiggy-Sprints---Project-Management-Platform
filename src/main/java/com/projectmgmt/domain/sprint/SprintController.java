package com.projectmgmt.domain.sprint;

import com.projectmgmt.common.model.ApiResponse;
import com.projectmgmt.common.util.SecurityUtils;
import com.projectmgmt.domain.sprint.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Sprints", description = "Sprint management and velocity tracking")
public class SprintController {

    private final SprintService sprintService;

    @PostMapping("/api/projects/{projectId}/sprints")
    @Operation(summary = "Create a new sprint in a project")
    public ResponseEntity<ApiResponse<SprintResponse>> create(@PathVariable UUID projectId,
                                                               @Valid @RequestBody CreateSprintRequest request) {
        SprintResponse response = sprintService.createSprint(projectId, request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @GetMapping("/api/projects/{projectId}/sprints")
    @Operation(summary = "List all sprints in a project")
    public ResponseEntity<ApiResponse<List<SprintResponse>>> list(@PathVariable UUID projectId) {
        List<SprintResponse> response = sprintService.getSprintsByProject(projectId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/api/sprints/{id}")
    @Operation(summary = "Get sprint by ID")
    public ResponseEntity<ApiResponse<SprintResponse>> get(@PathVariable UUID id) {
        SprintResponse response = sprintService.getSprint(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/api/sprints/{id}/start")
    @Operation(summary = "Start a planned sprint (transitions to ACTIVE)")
    public ResponseEntity<ApiResponse<SprintResponse>> start(@PathVariable UUID id) {
        SprintResponse response = sprintService.startSprint(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Sprint started successfully"));
    }

    @PostMapping("/api/sprints/{id}/complete")
    @Operation(summary = "Complete an active sprint with optional issue carry-over")
    public ResponseEntity<ApiResponse<SprintCompletionResult>> complete(@PathVariable UUID id,
                                                                        @Valid @RequestBody SprintCompleteRequest request) {
        SprintCompletionResult result = sprintService.completeSprint(id, request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(result, "Sprint completed successfully"));
    }

    @GetMapping("/api/projects/{projectId}/velocity")
    @Operation(summary = "Get velocity history for a project (completed sprints)")
    public ResponseEntity<ApiResponse<List<SprintResponse>>> velocity(@PathVariable UUID projectId) {
        List<SprintResponse> response = sprintService.getVelocityHistory(projectId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
