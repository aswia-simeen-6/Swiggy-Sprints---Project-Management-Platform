package com.projectmgmt.domain.issue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projectmgmt.common.exception.ResourceNotFoundException;
import com.projectmgmt.common.exception.ValidationException;
import com.projectmgmt.domain.issue.dto.*;
import com.projectmgmt.domain.project.Project;
import com.projectmgmt.domain.project.ProjectRepository;
import com.projectmgmt.domain.project.ProjectService;
import com.projectmgmt.domain.sprint.Sprint;
import com.projectmgmt.domain.sprint.SprintRepository;
import com.projectmgmt.domain.user.User;
import com.projectmgmt.domain.user.UserRepository;
import com.projectmgmt.domain.workflow.WorkflowStatus;
import com.projectmgmt.domain.workflow.WorkflowStatusRepository;
import com.projectmgmt.event.IssueCreatedEvent;
import com.projectmgmt.event.IssueUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final WorkflowStatusRepository statusRepository;
    private final SprintRepository sprintRepository;
    private final IssueWatcherRepository watcherRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    @Transactional
    public IssueResponse createIssue(UUID projectId, CreateIssueRequest request, UUID reporterId) {
        projectService.validateMembership(projectId, reporterId);

        // Lock project to atomically increment issue counter
        Project project = projectRepository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        // Validate parent for subtasks
        if (request.issueType() == IssueType.SUBTASK) {
            if (request.parentId() == null) {
                throw new ValidationException("Subtasks require a parent issue");
            }
            Issue parent = issueRepository.findByIdAndDeletedAtIsNull(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent issue", request.parentId()));
            if (parent.getIssueType() == IssueType.SUBTASK) {
                throw new ValidationException("Cannot create a subtask under another subtask");
            }
        }

        // Get default status (first TODO status)
        WorkflowStatus defaultStatus = statusRepository.findDefaultStatus(projectId)
                .orElseThrow(() -> new ValidationException("No default status found for project"));

        // Generate issue key atomically
        String issueKey = project.generateIssueKey();
        projectRepository.save(project);

        Issue issue = Issue.builder()
                .projectId(projectId)
                .issueKey(issueKey)
                .issueType(request.issueType())
                .title(request.title().trim())
                .description(request.description())
                .statusId(defaultStatus.getId())
                .priority(request.priority() != null ? request.priority() : Priority.MEDIUM)
                .assigneeId(request.assigneeId())
                .reporterId(reporterId)
                .sprintId(request.sprintId())
                .parentId(request.parentId())
                .storyPoints(request.storyPoints())
                .labels(request.labels() != null ? request.labels().toArray(new String[0]) : new String[]{})
                .customFields(request.customFields() != null ? request.customFields() : Map.of())
                .build();

        Issue saved = issueRepository.save(issue);

        // Auto-watch: reporter watches the issue
        watcherRepository.save(IssueWatcher.builder()
                .issueId(saved.getId())
                .userId(reporterId)
                .build());

        // Auto-watch: assignee watches the issue
        if (request.assigneeId() != null && !request.assigneeId().equals(reporterId)) {
            watcherRepository.save(IssueWatcher.builder()
                    .issueId(saved.getId())
                    .userId(request.assigneeId())
                    .build());
        }

        eventPublisher.publishEvent(new IssueCreatedEvent(saved, projectId, reporterId));

        log.info("Issue created: {} - {} in project {}", saved.getIssueKey(), saved.getTitle(), project.getKey());
        return toResponse(saved, defaultStatus);
    }

    @Transactional
    public IssueResponse updateIssue(UUID issueId, UpdateIssueRequest request, UUID userId) {
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        projectService.validateMembership(issue.getProjectId(), userId);

        // Track changes for activity log
        ObjectNode changes = objectMapper.createObjectNode();

        if (request.title() != null && !request.title().equals(issue.getTitle())) {
            changes.put("title_old", issue.getTitle());
            changes.put("title_new", request.title().trim());
            issue.setTitle(request.title().trim());
        }
        if (request.description() != null && !request.description().equals(issue.getDescription())) {
            changes.put("description", "updated");
            issue.setDescription(request.description());
        }
        if (request.priority() != null && request.priority() != issue.getPriority()) {
            changes.put("priority_old", issue.getPriority().name());
            changes.put("priority_new", request.priority().name());
            issue.setPriority(request.priority());
        }
        if (request.assigneeId() != null && !request.assigneeId().equals(issue.getAssigneeId())) {
            changes.put("assignee_old", issue.getAssigneeId() != null ? issue.getAssigneeId().toString() : "unassigned");
            changes.put("assignee_new", request.assigneeId().toString());
            issue.setAssigneeId(request.assigneeId());

            // Auto-watch for new assignee
            if (!watcherRepository.existsByIssueIdAndUserId(issueId, request.assigneeId())) {
                watcherRepository.save(IssueWatcher.builder()
                        .issueId(issueId)
                        .userId(request.assigneeId())
                        .build());
            }
        }
        if (request.sprintId() != null) {
            if (!request.sprintId().equals(issue.getSprintId())) {
                changes.put("sprint_old", issue.getSprintId() != null ? issue.getSprintId().toString() : "backlog");
                changes.put("sprint_new", request.sprintId().toString());
                issue.setSprintId(request.sprintId());
            }
        }
        if (request.storyPoints() != null) {
            changes.put("story_points_old", issue.getStoryPoints() != null ? issue.getStoryPoints() : 0);
            changes.put("story_points_new", request.storyPoints());
            issue.setStoryPoints(request.storyPoints());
        }
        if (request.labels() != null) {
            issue.setLabels(request.labels().toArray(new String[0]));
        }
        if (request.customFields() != null) {
            issue.setCustomFields(request.customFields());
        }

        Issue saved = issueRepository.save(issue);

        if (!changes.isEmpty()) {
            eventPublisher.publishEvent(new IssueUpdatedEvent(saved, saved.getProjectId(), changes, userId));
        }

        WorkflowStatus status = statusRepository.findByIdAndDeletedAtIsNull(saved.getStatusId()).orElse(null);
        return toResponse(saved, status);
    }

    @Transactional(readOnly = true)
    public IssueResponse getIssue(UUID issueId, UUID userId) {
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));
        projectService.validateMembership(issue.getProjectId(), userId);
        WorkflowStatus status = statusRepository.findByIdAndDeletedAtIsNull(issue.getStatusId()).orElse(null);
        return toResponse(issue, status);
    }

    @Transactional(readOnly = true)
    public IssueResponse getIssueByKey(String issueKey, UUID userId) {
        Issue issue = issueRepository.findByIssueKeyAndDeletedAtIsNull(issueKey)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "key", issueKey));
        projectService.validateMembership(issue.getProjectId(), userId);
        WorkflowStatus status = statusRepository.findByIdAndDeletedAtIsNull(issue.getStatusId()).orElse(null);
        return toResponse(issue, status);
    }

    @Transactional(readOnly = true)
    public BoardResponse getBoard(UUID projectId, UUID userId) {
        projectService.validateMembership(projectId, userId);

        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        List<WorkflowStatus> statuses = statusRepository.findByProjectIdOrderByPosition(projectId);

        // Get active sprint
        Sprint activeSprint = sprintRepository.findActiveSprintByProjectId(projectId).orElse(null);

        List<BoardResponse.BoardColumn> columns = statuses.stream().map(status -> {
            List<Issue> issues = issueRepository.findByProjectIdAndStatusId(projectId, status.getId());

            List<IssueResponse> issueResponses = issues.stream()
                    .map(i -> toResponse(i, status))
                    .toList();

            int totalPoints = issues.stream()
                    .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0)
                    .sum();

            return new BoardResponse.BoardColumn(
                    status.getId(), status.getName(), status.getCategory().name(),
                    status.getColor(), status.getPosition(),
                    issueResponses, issues.size(), totalPoints);
        }).toList();

        return new BoardResponse(
                projectId, project.getName(), project.getKey(), columns,
                activeSprint != null ? activeSprint.getId() : null,
                activeSprint != null ? activeSprint.getName() : null);
    }

    @Transactional
    public void deleteIssue(UUID issueId, UUID userId) {
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));
        projectService.validateMembership(issue.getProjectId(), userId);
        issue.softDelete();
        issueRepository.save(issue);
    }

    @Transactional
    public void watchIssue(UUID issueId, UUID userId) {
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));
        projectService.validateMembership(issue.getProjectId(), userId);

        if (!watcherRepository.existsByIssueIdAndUserId(issueId, userId)) {
            watcherRepository.save(IssueWatcher.builder().issueId(issueId).userId(userId).build());
        }
    }

    @Transactional
    public void unwatchIssue(UUID issueId, UUID userId) {
        watcherRepository.deleteByIssueIdAndUserId(issueId, userId);
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> getChildIssues(UUID parentId, UUID userId) {
        Issue parent = issueRepository.findByIdAndDeletedAtIsNull(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", parentId));
        projectService.validateMembership(parent.getProjectId(), userId);

        return issueRepository.findByParentId(parentId).stream()
                .map(i -> {
                    WorkflowStatus s = statusRepository.findByIdAndDeletedAtIsNull(i.getStatusId()).orElse(null);
                    return toResponse(i, s);
                })
                .toList();
    }

    public IssueResponse toResponse(Issue issue, WorkflowStatus status) {
        String assigneeName = null;
        if (issue.getAssigneeId() != null) {
            assigneeName = userRepository.findByIdAndDeletedAtIsNull(issue.getAssigneeId())
                    .map(User::getDisplayName)
                    .orElse(null);
        }
        return new IssueResponse(
                issue.getId(), issue.getProjectId(), issue.getIssueKey(),
                issue.getIssueType(), issue.getTitle(), issue.getDescription(),
                issue.getStatusId(),
                status != null ? status.getName() : null,
                status != null ? status.getCategory().name() : null,
                issue.getPriority(), issue.getAssigneeId(), assigneeName, issue.getReporterId(),
                issue.getSprintId(), issue.getParentId(), issue.getStoryPoints(),
                issue.getLabels() != null ? Arrays.asList(issue.getLabels()) : List.of(),
                issue.getCustomFields(), issue.getCreatedAt(), issue.getUpdatedAt(),
                issue.getVersion());
    }
}
