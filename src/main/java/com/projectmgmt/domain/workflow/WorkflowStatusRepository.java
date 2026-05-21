package com.projectmgmt.domain.workflow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowStatusRepository extends JpaRepository<WorkflowStatus, UUID> {

    @Query("SELECT ws FROM WorkflowStatus ws WHERE ws.projectId = :projectId AND ws.deletedAt IS NULL ORDER BY ws.position ASC")
    List<WorkflowStatus> findByProjectIdOrderByPosition(UUID projectId);

    Optional<WorkflowStatus> findByIdAndDeletedAtIsNull(UUID id);

    @Query("SELECT ws FROM WorkflowStatus ws WHERE ws.projectId = :projectId AND ws.category = 'TODO' AND ws.deletedAt IS NULL ORDER BY ws.position ASC")
    Optional<WorkflowStatus> findDefaultStatus(UUID projectId);
}
