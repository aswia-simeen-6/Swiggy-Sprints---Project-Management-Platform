package com.projectmgmt.domain.issue;

import com.projectmgmt.common.model.ApiResponse;
import com.projectmgmt.common.util.ETagUtils;
import com.projectmgmt.common.util.SecurityUtils;
import com.projectmgmt.domain.issue.dto.*;
import com.projectmgmt.domain.workflow.WorkflowEngine;
import com.projectmgmt.domain.workflow.WorkflowStatus;
import com.projectmgmt.domain.workflow.WorkflowStatusRepository;
import com.projectmgmt.domain.workflow.dto.WorkflowStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Issues", description = "Issue tracking and workflow transitions")
public class IssueController {

    private final IssueService issueService;
    private final WorkflowEngine workflowEngine;
    private final WorkflowStatusRepository statusRepository;

    @PostMapping("/api/projects/{projectId}/issues")
    @Operation(summary = "Create a new issue in a project")
    public ResponseEntity<ApiResponse<IssueResponse>> create(@PathVariable UUID projectId,
                                                              @Valid @RequestBody CreateIssueRequest request) {
        IssueResponse response = issueService.createIssue(projectId, request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @GetMapping("/api/projects/{projectId}/board")
    @Operation(summary = "Get the Kanban board state for a project")
    public ResponseEntity<BoardResponse> getBoard(@PathVariable UUID projectId,
                                                    @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        BoardResponse board = issueService.getBoard(projectId, SecurityUtils.getCurrentUserId());
        String etag = ETagUtils.generate(board.projectId(), board.columns().hashCode());
        return ETagUtils.withETag(board, etag, ifNoneMatch);
    }

    @GetMapping("/api/issues/{id}")
    @Operation(summary = "Get issue by ID")
    public ResponseEntity<ApiResponse<IssueResponse>> get(@PathVariable UUID id) {
        IssueResponse response = issueService.getIssue(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/api/issues/key/{issueKey}")
    @Operation(summary = "Get issue by key (e.g., PROJ-123)")
    public ResponseEntity<ApiResponse<IssueResponse>> getByKey(@PathVariable String issueKey) {
        IssueResponse response = issueService.getIssueByKey(issueKey, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/api/issues/{id}")
    @Operation(summary = "Update issue fields (supports optimistic locking via version)")
    public ResponseEntity<ApiResponse<IssueResponse>> update(@PathVariable UUID id,
                                                              @Valid @RequestBody UpdateIssueRequest request) {
        IssueResponse response = issueService.updateIssue(id, request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/api/issues/{id}/transitions")
    @Operation(summary = "Transition an issue to a new status (workflow engine)")
    @Transactional
    public ResponseEntity<ApiResponse<IssueResponse>> transition(@PathVariable UUID id,
                                                                   @Valid @RequestBody TransitionRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Issue issue = workflowEngine.transition(id, request.targetStatusId(), userId);
        WorkflowStatus status = statusRepository.findByIdAndDeletedAtIsNull(issue.getStatusId()).orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(issueService.toResponse(issue, status)));
    }

    @GetMapping("/api/issues/{id}/transitions")
    @Operation(summary = "Get allowed transitions for an issue from its current status")
    public ResponseEntity<ApiResponse<List<WorkflowStatusResponse>>> getAllowedTransitions(@PathVariable UUID id) {
        List<WorkflowStatus> allowed = workflowEngine.getAllowedTransitions(id);
        List<WorkflowStatusResponse> response = allowed.stream()
                .map(ws -> new WorkflowStatusResponse(ws.getId(), ws.getProjectId(),
                        ws.getName(), ws.getCategory().name(), ws.getPosition(), ws.getColor()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/api/issues/{id}")
    @Operation(summary = "Soft delete an issue")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        issueService.deleteIssue(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/issues/{id}/watch")
    @Operation(summary = "Watch an issue (subscribe to notifications)")
    public ResponseEntity<ApiResponse<Void>> watch(@PathVariable UUID id) {
        issueService.watchIssue(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Now watching this issue"));
    }

    @DeleteMapping("/api/issues/{id}/watch")
    @Operation(summary = "Unwatch an issue")
    public ResponseEntity<Void> unwatch(@PathVariable UUID id) {
        issueService.unwatchIssue(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/issues/{id}/children")
    @Operation(summary = "Get child issues (subtasks)")
    public ResponseEntity<ApiResponse<List<IssueResponse>>> getChildren(@PathVariable UUID id) {
        List<IssueResponse> children = issueService.getChildIssues(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(children));
    }
}
