package com.projectmgmt.domain.activity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ActivityRepository extends JpaRepository<ActivityLog, UUID> {

    @Query("SELECT a FROM ActivityLog a WHERE a.projectId = :projectId ORDER BY a.createdAt DESC")
    Slice<ActivityLog> findByProjectId(UUID projectId, Pageable pageable);

    @Query("SELECT a FROM ActivityLog a WHERE a.issueId = :issueId ORDER BY a.createdAt DESC")
    Slice<ActivityLog> findByIssueId(UUID issueId, Pageable pageable);

    @Query("SELECT a FROM ActivityLog a WHERE a.projectId = :projectId AND a.action = :action ORDER BY a.createdAt DESC")
    Slice<ActivityLog> findByProjectIdAndAction(UUID projectId, ActivityType action, Pageable pageable);

    // NOTE: No deleteById, no save-for-update. This repository is append-only by design.
    // The underlying table has DB-level triggers preventing UPDATE and DELETE.
}
