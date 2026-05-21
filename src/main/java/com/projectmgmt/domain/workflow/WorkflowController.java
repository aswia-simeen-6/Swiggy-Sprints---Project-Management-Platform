package com.projectmgmt.domain.workflow;

import com.projectmgmt.common.model.ApiResponse;
import com.projectmgmt.common.util.SecurityUtils;
import com.projectmgmt.domain.project.ProjectService;
import com.projectmgmt.domain.workflow.dto.WorkflowStatusResponse;
import com.projectmgmt.domain.workflow.dto.WorkflowTransitionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/workflow")
@RequiredArgsConstructor
@Tag(name = "Workflow", description = "Workflow configuration (statuses, transitions)")
public class WorkflowController {

    private final WorkflowStatusRepository statusRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final ProjectService projectService;

    @GetMapping("/statuses")
    @Operation(summary = "Get all workflow statuses for a project")
    public ResponseEntity<ApiResponse<List<WorkflowStatusResponse>>> getStatuses(@PathVariable UUID projectId) {
        projectService.validateMembership(projectId, SecurityUtils.getCurrentUserId());
        List<WorkflowStatusResponse> response = statusRepository.findByProjectIdOrderByPosition(projectId)
                .stream()
                .map(ws -> new WorkflowStatusResponse(ws.getId(), ws.getProjectId(),
                        ws.getName(), ws.getCategory().name(), ws.getPosition(), ws.getColor()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/transitions")
    @Operation(summary = "Get all workflow transitions for a project")
    public ResponseEntity<ApiResponse<List<WorkflowTransitionResponse>>> getTransitions(@PathVariable UUID projectId) {
        projectService.validateMembership(projectId, SecurityUtils.getCurrentUserId());
        List<WorkflowTransitionResponse> response = transitionRepository.findAllByProjectId(projectId)
                .stream()
                .map(wt -> new WorkflowTransitionResponse(
                        wt.getId(), wt.getName(),
                        wt.getFromStatusId(),
                        wt.getFromStatus() != null ? wt.getFromStatus().getName() : null,
                        wt.getToStatusId(),
                        wt.getToStatus() != null ? wt.getToStatus().getName() : null,
                        wt.getConditions().stream()
                                .map(c -> new WorkflowTransitionResponse.ConditionResponse(
                                        c.getId(), c.getConditionType(), c.getConfig()))
                                .toList(),
                        wt.getActions().stream()
                                .map(a -> new WorkflowTransitionResponse.ActionResponse(
                                        a.getId(), a.getActionType(), a.getConfig()))
                                .toList()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
