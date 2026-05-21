package com.projectmgmt.domain.workflow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, UUID> {

    @Query("SELECT wt FROM WorkflowTransition wt " +
           "LEFT JOIN FETCH wt.conditions " +
           "LEFT JOIN FETCH wt.actions " +
           "WHERE wt.fromStatusId = :fromStatusId AND wt.toStatusId = :toStatusId")
    Optional<WorkflowTransition> findByFromStatusIdAndToStatusId(UUID fromStatusId, UUID toStatusId);

    @Query("SELECT wt FROM WorkflowTransition wt " +
           "JOIN FETCH wt.toStatus " +
           "WHERE wt.fromStatusId = :fromStatusId")
    List<WorkflowTransition> findAllByFromStatusId(UUID fromStatusId);

    @Query("SELECT wt FROM WorkflowTransition wt WHERE wt.projectId = :projectId")
    List<WorkflowTransition> findAllByProjectId(UUID projectId);

    @Query("SELECT DISTINCT ws FROM WorkflowTransition wt JOIN wt.toStatus ws WHERE wt.fromStatusId = :fromStatusId")
    List<WorkflowStatus> findAllowedTargetStatuses(UUID fromStatusId);
}
