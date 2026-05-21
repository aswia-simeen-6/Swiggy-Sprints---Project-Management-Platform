package com.projectmgmt.domain.comment;

import com.projectmgmt.common.model.ApiResponse;
import com.projectmgmt.common.util.SecurityUtils;
import com.projectmgmt.domain.comment.dto.*;
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
@RequestMapping("/api/issues/{issueId}/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Threaded comments with @mentions")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "Add a comment to an issue (supports threading via parentId)")
    public ResponseEntity<ApiResponse<CommentResponse>> add(@PathVariable UUID issueId,
                                                             @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse response = commentService.addComment(issueId, request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @GetMapping
    @Operation(summary = "List comments on an issue (threaded, paginated)")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> list(@PathVariable UUID issueId,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        List<CommentResponse> response = commentService.getComments(issueId, SecurityUtils.getCurrentUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete a comment (soft delete, author only)")
    public ResponseEntity<Void> delete(@PathVariable UUID issueId, @PathVariable UUID commentId) {
        commentService.deleteComment(commentId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
