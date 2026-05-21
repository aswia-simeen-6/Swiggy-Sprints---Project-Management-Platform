package com.projectmgmt.domain.search;

import com.projectmgmt.common.model.ApiResponse;
import com.projectmgmt.common.model.CursorPage;
import com.projectmgmt.common.util.SecurityUtils;
import com.projectmgmt.domain.issue.Priority;
import com.projectmgmt.domain.issue.dto.IssueResponse;
import com.projectmgmt.domain.search.dto.SearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Full-text search and structured filtering")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Search issues with full-text search and structured filters")
    public ResponseEntity<ApiResponse<CursorPage<IssueResponse>>> search(
            @RequestParam UUID projectId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID statusId,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) List<String> labels,
            @RequestParam(required = false) UUID sprintId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {

        SearchRequest request = new SearchRequest(q, statusId, assigneeId, priority, labels, sprintId, cursor, limit);
        CursorPage<IssueResponse> result = searchService.search(projectId, request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
