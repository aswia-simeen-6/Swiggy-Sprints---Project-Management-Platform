package com.projectmgmt.domain.sprint;

import com.projectmgmt.common.exception.ResourceNotFoundException;
import com.projectmgmt.common.exception.ValidationException;
import com.projectmgmt.domain.activity.ActivityLog;
import com.projectmgmt.domain.activity.ActivityRepository;
import com.projectmgmt.domain.activity.ActivityType;
import com.projectmgmt.domain.issue.Issue;
import com.projectmgmt.domain.issue.IssueRepository;
import com.projectmgmt.domain.project.ProjectService;
import com.projectmgmt.domain.sprint.dto.*;
import com.projectmgmt.domain.workflow.StatusCategory;
import com.projectmgmt.domain.workflow.WorkflowStatus;
import com.projectmgmt.domain.workflow.WorkflowStatusRepository;
import com.projectmgmt.event.SprintUpdatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepository sprintRepository;
    private final IssueRepository issueRepository;
    private final ProjectService projectService;
    private final WorkflowStatusRepository statusRepository;
    private final ActivityRepository activityRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public SprintResponse createSprint(UUID projectId, CreateSprintRequest request, UUID userId) {
        projectService.validateMembership(projectId, userId);

        if (request.startDate() != null && request.endDate() != null
                && request.endDate().isBefore(request.startDate())) {
            throw new ValidationException("End date must be after start date");
        }

        Sprint sprint = Sprint.builder()
                .projectId(projectId)
                .name(request.name().trim())
                .goal(request.goal())
                .status(SprintStatus.PLANNED)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build();

        Sprint saved = sprintRepository.save(sprint);

        eventPublisher.publishEvent(new SprintUpdatedEvent(saved, projectId, "CREATED", userId));

        log.info("Sprint created: {} in project {}", saved.getName(), projectId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> getSprintsByProject(UUID projectId, UUID userId) {
        projectService.validateMembership(projectId, userId);
        return sprintRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SprintResponse getSprint(UUID sprintId, UUID userId) {
        Sprint sprint = sprintRepository.findByIdAndDeletedAtIsNull(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", sprintId));
        projectService.validateMembership(sprint.getProjectId(), userId);
        return toResponse(sprint);
    }

    @Transactional
    public SprintResponse startSprint(UUID sprintId, UUID userId) {
        Sprint sprint = sprintRepository.findByIdForUpdate(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", sprintId));

        projectService.validateMembership(sprint.getProjectId(), userId);

        if (sprint.getStatus() != SprintStatus.PLANNED) {
            throw new ValidationException("Only planned sprints can be started. Current status: " + sprint.getStatus());
        }

        // Check no other active sprint exists
        if (sprintRepository.hasActiveSprint(sprint.getProjectId())) {
            throw new ValidationException("An active sprint already exists in this project. Complete it before starting a new one.");
        }

        sprint.setStatus(SprintStatus.ACTIVE);
        sprint.setStartedAt(Instant.now());
        Sprint saved = sprintRepository.save(sprint);

        eventPublisher.publishEvent(new SprintUpdatedEvent(saved, saved.getProjectId(), "STARTED", userId));

        log.info("Sprint started: {}", saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public SprintCompletionResult completeSprint(UUID sprintId, SprintCompleteRequest request, UUID userId) {
        Sprint sprint = sprintRepository.findByIdForUpdate(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", sprintId));

        projectService.validateMembership(sprint.getProjectId(), userId);

        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new ValidationException("Only active sprints can be completed. Current status: " + sprint.getStatus());
        }

        // 1. Fetch all issues in sprint
        List<Issue> allIssues = issueRepository.findBySprintId(sprintId);

        // 2. Build status lookup for categorization
        Map<UUID, WorkflowStatus> statusMap = statusRepository.findByProjectIdOrderByPosition(sprint.getProjectId())
                .stream()
                .collect(Collectors.toMap(WorkflowStatus::getId, s -> s));

        // 3. Partition into completed vs incomplete
        Map<Boolean, List<Issue>> partitioned = allIssues.stream()
                .collect(Collectors.partitioningBy(issue -> {
                    WorkflowStatus status = statusMap.get(issue.getStatusId());
                    return status != null && status.getCategory() == StatusCategory.DONE;
                }));

        List<Issue> completedIssues = partitioned.get(true);
        List<Issue> incompleteIssues = partitioned.get(false);

        // 4. Calculate velocity
        int velocity = completedIssues.stream()
                .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0)
                .sum();

        int totalPoints = allIssues.stream()
                .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0)
                .sum();

        // 5. Carry over selected issues
        Set<UUID> carryOverIds = new HashSet<>(request.carryOverIssueIds());
        Set<UUID> incompleteIds = incompleteIssues.stream().map(Issue::getId).collect(Collectors.toSet());

        // Validate carry-over IDs are actually incomplete issues in this sprint
        Set<UUID> invalidIds = carryOverIds.stream()
                .filter(id -> !incompleteIds.contains(id))
                .collect(Collectors.toSet());
        if (!invalidIds.isEmpty()) {
            throw new ValidationException("Issues not eligible for carry-over (not in sprint or already completed): " + invalidIds);
        }

        // Validate target sprint if carry-over requested
        if (!carryOverIds.isEmpty() && request.targetSprintId() != null) {
            Sprint targetSprint = sprintRepository.findByIdAndDeletedAtIsNull(request.targetSprintId())
                    .orElseThrow(() -> new ResourceNotFoundException("Target Sprint", request.targetSprintId()));
            if (targetSprint.getStatus() == SprintStatus.COMPLETED) {
                throw new ValidationException("Cannot carry over issues to a completed sprint");
            }
        }

        int carriedOver = 0;
        for (Issue issue : incompleteIssues) {
            if (carryOverIds.contains(issue.getId())) {
                issue.setSprintId(request.targetSprintId()); // Move to target sprint
                carriedOver++;

                activityRepository.save(ActivityLog.builder()
                        .projectId(sprint.getProjectId())
                        .issueId(issue.getId())
                        .userId(userId)
                        .action(ActivityType.ISSUE_MOVED_TO_SPRINT)
                        .changes(buildNode(Map.of(
                                "fromSprint", sprint.getName(),
                                "toSprint", request.targetSprintId() != null ? request.targetSprintId().toString() : "backlog")))
                        .build());
            } else {
                issue.setSprintId(null); // Back to backlog

                activityRepository.save(ActivityLog.builder()
                        .projectId(sprint.getProjectId())
                        .issueId(issue.getId())
                        .userId(userId)
                        .action(ActivityType.ISSUE_MOVED_TO_BACKLOG)
                        .changes(buildNode(Map.of("fromSprint", sprint.getName())))
                        .build());
            }
            issueRepository.save(issue);
        }

        // 6. Complete sprint
        sprint.setStatus(SprintStatus.COMPLETED);
        sprint.setCompletedAt(Instant.now());
        sprint.setVelocity(velocity);
        sprint.setCompletedPoints(velocity);
        sprint.setTotalPoints(totalPoints);
        Sprint saved = sprintRepository.save(sprint);

        eventPublisher.publishEvent(new SprintUpdatedEvent(saved, saved.getProjectId(), "COMPLETED", userId));

        log.info("Sprint completed: {} | velocity={} | completed={} | incomplete={} | carried={}",
                saved.getName(), velocity, completedIssues.size(), incompleteIssues.size(), carriedOver);

        return new SprintCompletionResult(
                toResponse(saved),
                completedIssues.size(),
                incompleteIssues.size(),
                carriedOver,
                velocity);
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> getVelocityHistory(UUID projectId, UUID userId) {
        projectService.validateMembership(projectId, userId);
        return sprintRepository.findCompletedSprintsByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    private SprintResponse toResponse(Sprint sprint) {
        return new SprintResponse(
                sprint.getId(), sprint.getProjectId(), sprint.getName(), sprint.getGoal(),
                sprint.getStatus(), sprint.getStartDate(), sprint.getEndDate(),
                sprint.getStartedAt(), sprint.getCompletedAt(), sprint.getVelocity(),
                sprint.getCompletedPoints(), sprint.getTotalPoints(),
                sprint.getCreatedAt(), sprint.getUpdatedAt(), sprint.getVersion());
    }

    private ObjectNode buildNode(Map<String, String> fields) {
        ObjectNode node = objectMapper.createObjectNode();
        fields.forEach(node::put);
        return node;
    }
}
