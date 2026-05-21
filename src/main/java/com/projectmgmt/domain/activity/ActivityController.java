package com.projectmgmt.domain.activity;

import com.projectmgmt.common.model.ApiResponse;
import com.projectmgmt.common.util.SecurityUtils;
import com.projectmgmt.domain.activity.dto.ActivityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Activity", description = "Activity feed and audit trail")
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/api/projects/{projectId}/activity")
    @Operation(summary = "Get project activity feed (paginated, filterable)")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> projectActivity(
            @PathVariable UUID projectId,
            @RequestParam(required = false) ActivityType action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ActivityResponse> response = activityService.getProjectActivity(
                projectId, SecurityUtils.getCurrentUserId(), action, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/api/issues/{issueId}/activity")
    @Operation(summary = "Get issue-specific activity history")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> issueActivity(
            @PathVariable UUID issueId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ActivityResponse> response = activityService.getIssueActivity(
                issueId, SecurityUtils.getCurrentUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
