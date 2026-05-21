package com.projectmgmt.domain.comment;

import com.projectmgmt.common.exception.ResourceNotFoundException;
import com.projectmgmt.domain.comment.dto.*;
import com.projectmgmt.domain.issue.Issue;
import com.projectmgmt.domain.issue.IssueRepository;
import com.projectmgmt.domain.project.ProjectService;
import com.projectmgmt.domain.user.User;
import com.projectmgmt.domain.user.UserRepository;
import com.projectmgmt.event.CommentAddedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@\\[([^]]+)]\\(([0-9a-f-]+)\\)");

    private final CommentRepository commentRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CommentResponse addComment(UUID issueId, CreateCommentRequest request, UUID authorId) {
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        projectService.validateMembership(issue.getProjectId(), authorId);

        // Validate parent comment exists if threading
        if (request.parentId() != null) {
            commentRepository.findByIdAndDeletedAtIsNull(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment", request.parentId()));
        }

        // Parse @mentions from body
        UUID[] mentionedIds = extractMentions(request.body());

        Comment comment = Comment.builder()
                .issueId(issueId)
                .authorId(authorId)
                .parentId(request.parentId())
                .body(request.body())
                .mentions(mentionedIds)
                .build();

        Comment saved = commentRepository.save(comment);

        eventPublisher.publishEvent(new CommentAddedEvent(saved, issue.getProjectId(), issueId, authorId));

        User author = userRepository.findByIdAndDeletedAtIsNull(authorId).orElse(null);
        return toResponse(saved, author != null ? author.getDisplayName() : null);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(UUID issueId, UUID userId, int page, int size) {
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        projectService.validateMembership(issue.getProjectId(), userId);

        Slice<Comment> topLevel = commentRepository.findTopLevelByIssueId(issueId, PageRequest.of(page, size));

        // Build user lookup
        Set<UUID> authorIds = new HashSet<>();
        topLevel.forEach(c -> authorIds.add(c.getAuthorId()));
        Map<UUID, String> userNames = buildUserNameMap(authorIds);

        return topLevel.stream()
                .map(comment -> {
                    List<Comment> replies = commentRepository.findRepliesByParentId(comment.getId());
                    replies.forEach(r -> authorIds.add(r.getAuthorId()));
                    Map<UUID, String> allNames = buildUserNameMap(authorIds);

                    List<CommentResponse> replyResponses = replies.stream()
                            .map(r -> toResponse(r, allNames.get(r.getAuthorId())))
                            .toList();

                    return toResponseWithReplies(comment, allNames.get(comment.getAuthorId()), replyResponses);
                })
                .toList();
    }

    @Transactional
    public void deleteComment(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        if (!comment.getAuthorId().equals(userId)) {
            throw new com.projectmgmt.common.exception.ForbiddenException("You can only delete your own comments");
        }

        comment.softDelete();
        commentRepository.save(comment);
    }

    private UUID[] extractMentions(String body) {
        if (body == null) return new UUID[]{};
        Matcher matcher = MENTION_PATTERN.matcher(body);
        Set<UUID> mentions = new LinkedHashSet<>();
        while (matcher.find()) {
            try {
                mentions.add(UUID.fromString(matcher.group(2)));
            } catch (IllegalArgumentException ignored) {
                // Skip invalid UUIDs
            }
        }
        return mentions.toArray(new UUID[0]);
    }

    private Map<UUID, String> buildUserNameMap(Set<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();
        Map<UUID, String> map = new HashMap<>();
        userRepository.findAllById(userIds).forEach(u -> map.put(u.getId(), u.getDisplayName()));
        return map;
    }

    private CommentResponse toResponse(Comment comment, String authorName) {
        return new CommentResponse(
                comment.getId(), comment.getIssueId(), comment.getAuthorId(),
                authorName, comment.getParentId(), comment.getBody(),
                comment.getMentions() != null ? Arrays.asList(comment.getMentions()) : List.of(),
                List.of(), comment.getCreatedAt(), comment.getUpdatedAt(), comment.getVersion());
    }

    private CommentResponse toResponseWithReplies(Comment comment, String authorName, List<CommentResponse> replies) {
        return new CommentResponse(
                comment.getId(), comment.getIssueId(), comment.getAuthorId(),
                authorName, comment.getParentId(), comment.getBody(),
                comment.getMentions() != null ? Arrays.asList(comment.getMentions()) : List.of(),
                replies, comment.getCreatedAt(), comment.getUpdatedAt(), comment.getVersion());
    }
}
