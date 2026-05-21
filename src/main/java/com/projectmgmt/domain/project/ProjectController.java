package com.projectmgmt.domain.project;

import com.projectmgmt.common.model.ApiResponse;
import com.projectmgmt.common.util.SecurityUtils;
import com.projectmgmt.domain.project.dto.*;
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
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "Create a new project")
    public ResponseEntity<ApiResponse<ProjectResponse>> create(@Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.createProject(request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ApiResponse<ProjectResponse>> get(@PathVariable UUID id) {
        ProjectResponse response = projectService.getProject(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "Get all projects for the current user")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> list() {
        List<ProjectResponse> response = projectService.getUserProjects(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add a member to a project")
    public ResponseEntity<ApiResponse<Void>> addMember(@PathVariable UUID id,
                                                        @RequestParam UUID userId,
                                                        @RequestParam(required = false) String role) {
        projectService.addMember(id, userId, role, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Member added successfully"));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List all members of a project")
    public ResponseEntity<ApiResponse<List<MemberDetailResponse>>> listMembers(@PathVariable UUID id) {
        List<MemberDetailResponse> members = projectService.getProjectMembers(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(members));
    }
}
