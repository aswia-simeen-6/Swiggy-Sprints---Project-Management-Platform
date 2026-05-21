package com.projectmgmt.domain.issue;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueRepository extends JpaRepository<Issue, UUID> {

    Optional<Issue> findByIdAndDeletedAtIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Issue i WHERE i.id = :id AND i.deletedAt IS NULL")
    Optional<Issue> findByIdForUpdate(UUID id);

    Optional<Issue> findByIssueKeyAndDeletedAtIsNull(String issueKey);

    @Query("SELECT i FROM Issue i WHERE i.projectId = :projectId AND i.deletedAt IS NULL ORDER BY i.createdAt DESC")
    Slice<Issue> findByProjectId(UUID projectId, Pageable pageable);

    @Query("SELECT i FROM Issue i WHERE i.projectId = :projectId AND i.statusId = :statusId AND i.deletedAt IS NULL ORDER BY i.createdAt DESC")
    List<Issue> findByProjectIdAndStatusId(UUID projectId, UUID statusId);

    @Query("SELECT i FROM Issue i WHERE i.sprintId = :sprintId AND i.deletedAt IS NULL")
    List<Issue> findBySprintId(UUID sprintId);

    @Query("SELECT i FROM Issue i WHERE i.parentId = :parentId AND i.deletedAt IS NULL ORDER BY i.createdAt ASC")
    List<Issue> findByParentId(UUID parentId);

    @Query("SELECT i FROM Issue i WHERE i.projectId = :projectId AND i.sprintId IS NULL AND i.deletedAt IS NULL ORDER BY i.createdAt DESC")
    Slice<Issue> findBacklogByProjectId(UUID projectId, Pageable pageable);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.projectId = :projectId AND i.deletedAt IS NULL")
    long countByProjectId(UUID projectId);
}
