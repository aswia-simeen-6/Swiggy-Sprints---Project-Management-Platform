package com.projectmgmt.domain.workflow;

import com.projectmgmt.common.exception.ResourceNotFoundException;
import com.projectmgmt.common.exception.WorkflowViolationException;
import com.projectmgmt.domain.issue.Issue;
import com.projectmgmt.domain.issue.IssueRepository;
import com.projectmgmt.domain.issue.dto.IssueResponse;
import com.projectmgmt.domain.workflow.engine.*;
import com.projectmgmt.event.IssueTransitionedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEngine {

    private final IssueRepository issueRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final WorkflowStatusRepository statusRepository;
    private final ConditionRegistry conditionRegistry;
    private final ActionRegistry actionRegistry;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Issue transition(UUID issueId, UUID targetStatusId, UUID userId) {
        // 1. Lock the issue row (SELECT FOR UPDATE)
        Issue issue = issueRepository.findByIdForUpdate(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        // 2. Validate target status exists
        WorkflowStatus targetStatus = statusRepository.findByIdAndDeletedAtIsNull(targetStatusId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowStatus", targetStatusId));

        // 3. No-op if already in target status
        if (issue.getStatusId().equals(targetStatusId)) {
            log.debug("Issue {} is already in status {}", issue.getIssueKey(), targetStatus.getName());
            return issue;
        }

        // 4. Find the transition rule
        WorkflowTransition transition = transitionRepository
                .findByFromStatusIdAndToStatusId(issue.getStatusId(), targetStatusId)
                .orElseThrow(() -> {
                    List<String> allowed = transitionRepository
                            .findAllowedTargetStatuses(issue.getStatusId())
                            .stream()
                            .map(WorkflowStatus::getName)
                            .toList();
                    return new WorkflowViolationException(
                            issue.getStatus() != null ? issue.getStatus().getName() : issue.getStatusId().toString(),
                            targetStatus.getName(),
                            allowed);
                });

        // 5. Evaluate conditions pipeline — short-circuit on first failure
        for (TransitionCondition condition : transition.getConditions()) {
            ConditionEvaluator evaluator = conditionRegistry.get(condition.getConditionType());
            if (evaluator != null) {
                evaluator.evaluate(issue, condition.getConfig());
            }
        }

        // 6. Capture old status name for event
        String oldStatusName = issue.getStatus() != null ? issue.getStatus().getName() : "Unknown";

        // 7. Apply the transition
        issue.setStatusId(targetStatusId);
        Issue saved = issueRepository.save(issue);

        // 8. Execute post-transition actions
        for (TransitionAction action : transition.getActions()) {
            TransitionActionExecutor executor = actionRegistry.get(action.getActionType());
            if (executor != null) {
                try {
                    executor.execute(saved, action.getConfig(), userId);
                } catch (Exception e) {
                    // Action failures should not block the transition
                    log.error("Post-transition action {} failed for issue {}: {}",
                            action.getActionType(), saved.getIssueKey(), e.getMessage());
                }
            }
        }

        // 9. Fire domain event — listeners handle activity log, notifications, WS broadcast
        eventPublisher.publishEvent(new IssueTransitionedEvent(
                saved, saved.getProjectId(), oldStatusName, targetStatus.getName(),
                transition.getId(), userId));

        log.info("Issue {} transitioned: {} → {} by user {}",
                saved.getIssueKey(), oldStatusName, targetStatus.getName(), userId);

        return saved;
    }

    public List<WorkflowStatus> getAllowedTransitions(UUID issueId) {
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));
        return transitionRepository.findAllowedTargetStatuses(issue.getStatusId());
    }
}
