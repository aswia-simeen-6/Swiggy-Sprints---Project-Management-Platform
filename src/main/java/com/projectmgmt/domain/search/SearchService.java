package com.projectmgmt.domain.search;

import com.projectmgmt.common.model.CursorPage;
import com.projectmgmt.common.util.CursorUtils;
import com.projectmgmt.domain.issue.dto.IssueResponse;
import com.projectmgmt.domain.project.ProjectService;
import com.projectmgmt.domain.search.dto.SearchRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final EntityManager entityManager;
    private final ProjectService projectService;

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public CursorPage<IssueResponse> search(UUID projectId, SearchRequest request, UUID userId) {
        projectService.validateMembership(projectId, userId);

        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        boolean hasTextQuery = request.query() != null && !request.query().isBlank();

        if (hasTextQuery) {
            sql.append("SELECT i.*, ts_rank(i.search_vector, plainto_tsquery('english', :queryText)) AS rank ");
        } else {
            sql.append("SELECT i.*, 0 AS rank ");
        }

        sql.append("FROM issues i ");
        sql.append("LEFT JOIN workflow_statuses ws ON ws.id = i.status_id ");
        sql.append("WHERE i.project_id = :projectId AND i.deleted_at IS NULL ");
        params.put("projectId", projectId);

        if (hasTextQuery) {
            sql.append("AND i.search_vector @@ plainto_tsquery('english', :queryText) ");
            params.put("queryText", request.query());
        }

        if (request.statusId() != null) {
            sql.append("AND i.status_id = :statusId ");
            params.put("statusId", request.statusId());
        }
        if (request.assigneeId() != null) {
            sql.append("AND i.assignee_id = :assigneeId ");
            params.put("assigneeId", request.assigneeId());
        }
        if (request.priority() != null) {
            sql.append("AND i.priority = :priority ");
            params.put("priority", request.priority().name());
        }
        if (request.sprintId() != null) {
            sql.append("AND i.sprint_id = :sprintId ");
            params.put("sprintId", request.sprintId());
        }
        if (request.labels() != null && !request.labels().isEmpty()) {
            sql.append("AND i.labels && :labels ");
            params.put("labels", request.labels().toArray(new String[0]));
        }

        // Cursor pagination
        if (request.cursor() != null && !request.cursor().isBlank()) {
            CursorUtils.CursorData cursor = CursorUtils.decode(request.cursor());
            sql.append("AND (i.created_at, i.id) < (:cursorTime, :cursorId) ");
            params.put("cursorTime", Timestamp.from(cursor.timestamp()));
            params.put("cursorId", cursor.id());
        }

        if (hasTextQuery) {
            sql.append("ORDER BY rank DESC, i.created_at DESC, i.id DESC ");
        } else {
            sql.append("ORDER BY i.created_at DESC, i.id DESC ");
        }

        int fetchLimit = request.limit() + 1; // Extra to determine hasMore
        sql.append("LIMIT :fetchLimit ");
        params.put("fetchLimit", fetchLimit);

        Query query = entityManager.createNativeQuery(sql.toString(), com.projectmgmt.domain.issue.Issue.class);
        params.forEach(query::setParameter);

        List<com.projectmgmt.domain.issue.Issue> results = query.getResultList();

        boolean hasMore = results.size() > request.limit();
        if (hasMore) {
            results = results.subList(0, request.limit());
        }

        List<IssueResponse> items = results.stream()
                .map(issue -> new IssueResponse(
                        issue.getId(), issue.getProjectId(), issue.getIssueKey(),
                        issue.getIssueType(), issue.getTitle(), issue.getDescription(),
                        issue.getStatusId(), null, null,
                        issue.getPriority(), issue.getAssigneeId(), null,issue.getReporterId(),
                        issue.getSprintId(), issue.getParentId(), issue.getStoryPoints(),
                        issue.getLabels() != null ? Arrays.asList(issue.getLabels()) : List.of(),
                        issue.getCustomFields(), issue.getCreatedAt(), issue.getUpdatedAt(),
                        issue.getVersion()))
                .toList();

        String nextCursor = null;
        if (hasMore && !items.isEmpty()) {
            IssueResponse last = items.getLast();
            nextCursor = CursorUtils.encode(last.createdAt(), last.id());
        }

        return CursorPage.of(items, nextCursor, hasMore, -1); // Total count is expensive, skip for search
    }
}
