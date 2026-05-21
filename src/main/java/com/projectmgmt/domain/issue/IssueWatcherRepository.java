package com.projectmgmt.domain.issue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface IssueWatcherRepository extends JpaRepository<IssueWatcher, UUID> {

    boolean existsByIssueIdAndUserId(UUID issueId, UUID userId);

    @Modifying
    @Query("DELETE FROM IssueWatcher iw WHERE iw.issueId = :issueId AND iw.userId = :userId")
    void deleteByIssueIdAndUserId(UUID issueId, UUID userId);

    @Query("SELECT iw.userId FROM IssueWatcher iw WHERE iw.issueId = :issueId")
    Set<UUID> findWatcherIdsByIssueId(UUID issueId);
}
