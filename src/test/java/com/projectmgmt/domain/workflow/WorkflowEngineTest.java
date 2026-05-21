package com.projectmgmt.domain.workflow;

import com.projectmgmt.common.exception.ResourceNotFoundException;
import com.projectmgmt.common.exception.WorkflowViolationException;
import com.projectmgmt.domain.issue.Issue;
import com.projectmgmt.domain.issue.IssueRepository;
import com.projectmgmt.domain.issue.IssueType;
import com.projectmgmt.domain.issue.Priority;
import com.projectmgmt.domain.workflow.engine.ActionRegistry;
import com.projectmgmt.domain.workflow.engine.ConditionEvaluator;
import com.projectmgmt.domain.workflow.engine.ConditionRegistry;
import com.projectmgmt.event.IssueTransitionedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowEngineTest {

    @Mock private IssueRepository issueRepository;
    @Mock private WorkflowTransitionRepository transitionRepository;
    @Mock private WorkflowStatusRepository statusRepository;
    @Mock private ConditionRegistry conditionRegistry;
    @Mock private ActionRegistry actionRegistry;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private WorkflowEngine engine;

    private UUID issueId;
    private UUID fromStatusId;
    private UUID toStatusId;
    private UUID userId;
    private Issue testIssue;
    private WorkflowStatus fromStatus;
    private WorkflowStatus toStatus;
    private WorkflowTransition transition;

    @BeforeEach
    void setUp() {
        issueId = UUID.randomUUID();
        fromStatusId = UUID.randomUUID();
        toStatusId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testIssue = Issue.builder()
                .projectId(UUID.randomUUID())
                .issueKey("TEST-1")
                .issueType(IssueType.TASK)
                .title("Test issue")
                .statusId(fromStatusId)
                .priority(Priority.MEDIUM)
                .reporterId(userId)
                .build();

        fromStatus = WorkflowStatus.builder()
                .name("To Do")
                .category(StatusCategory.TODO)
                .projectId(testIssue.getProjectId())
                .position(0)
                .build();

        toStatus = WorkflowStatus.builder()
                .name("In Progress")
                .category(StatusCategory.IN_PROGRESS)
                .projectId(testIssue.getProjectId())
                .position(1)
                .build();

        transition = WorkflowTransition.builder()
                .projectId(testIssue.getProjectId())
                .fromStatusId(fromStatusId)
                .toStatusId(toStatusId)
                .name("Start Work")
                .build();
    }

    @Test
    void transition_validTransition_shouldUpdateStatusAndFireEvent() {
        when(issueRepository.findByIdForUpdate(issueId)).thenReturn(Optional.of(testIssue));
        when(statusRepository.findByIdAndDeletedAtIsNull(toStatusId)).thenReturn(Optional.of(toStatus));
        when(transitionRepository.findByFromStatusIdAndToStatusId(fromStatusId, toStatusId))
                .thenReturn(Optional.of(transition));
        when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));

        Issue result = engine.transition(issueId, toStatusId, userId);

        assertThat(result.getStatusId()).isEqualTo(toStatusId);
        verify(issueRepository).save(testIssue);

        ArgumentCaptor<IssueTransitionedEvent> eventCaptor = ArgumentCaptor.forClass(IssueTransitionedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getToStatus()).isEqualTo("In Progress");
    }

    @Test
    void transition_sameStatus_shouldReturnWithoutChange() {
        testIssue.setStatusId(toStatusId);
        when(issueRepository.findByIdForUpdate(issueId)).thenReturn(Optional.of(testIssue));
        when(statusRepository.findByIdAndDeletedAtIsNull(toStatusId)).thenReturn(Optional.of(toStatus));

        Issue result = engine.transition(issueId, toStatusId, userId);

        assertThat(result.getStatusId()).isEqualTo(toStatusId);
        verify(issueRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void transition_issueNotFound_shouldThrow() {
        when(issueRepository.findByIdForUpdate(issueId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> engine.transition(issueId, toStatusId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void transition_noTransitionRule_shouldThrowWithAllowedStatuses() {
        when(issueRepository.findByIdForUpdate(issueId)).thenReturn(Optional.of(testIssue));
        when(statusRepository.findByIdAndDeletedAtIsNull(toStatusId)).thenReturn(Optional.of(toStatus));
        when(transitionRepository.findByFromStatusIdAndToStatusId(fromStatusId, toStatusId))
                .thenReturn(Optional.empty());
        when(transitionRepository.findAllowedTargetStatuses(fromStatusId))
                .thenReturn(List.of(fromStatus));

        assertThatThrownBy(() -> engine.transition(issueId, toStatusId, userId))
                .isInstanceOf(WorkflowViolationException.class);
    }

    @Test
    void transition_conditionFails_shouldThrow() {
        TransitionCondition condition = TransitionCondition.builder()
                .conditionType("FIELD_REQUIRED")
                .config(null)
                .build();
        transition.setConditions(List.of(condition));

        when(issueRepository.findByIdForUpdate(issueId)).thenReturn(Optional.of(testIssue));
        when(statusRepository.findByIdAndDeletedAtIsNull(toStatusId)).thenReturn(Optional.of(toStatus));
        when(transitionRepository.findByFromStatusIdAndToStatusId(fromStatusId, toStatusId))
                .thenReturn(Optional.of(transition));

        ConditionEvaluator mockEvaluator = mock(ConditionEvaluator.class);
        when(conditionRegistry.get("FIELD_REQUIRED")).thenReturn(mockEvaluator);
        doThrow(new WorkflowViolationException("To Do", "In Progress", List.of()))
                .when(mockEvaluator).evaluate(any(), any());

        assertThatThrownBy(() -> engine.transition(issueId, toStatusId, userId))
                .isInstanceOf(WorkflowViolationException.class);
    }

    @Test
    void getAllowedTransitions_shouldReturnTargetStatuses() {
        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(testIssue));
        when(transitionRepository.findAllowedTargetStatuses(fromStatusId)).thenReturn(List.of(toStatus));

        List<WorkflowStatus> result = engine.getAllowedTransitions(issueId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("In Progress");
    }
}
