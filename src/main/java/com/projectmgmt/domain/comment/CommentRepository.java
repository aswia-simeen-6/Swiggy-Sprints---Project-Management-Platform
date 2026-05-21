package com.projectmgmt.domain.comment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    Optional<Comment> findByIdAndDeletedAtIsNull(UUID id);

    @Query("SELECT c FROM Comment c WHERE c.issueId = :issueId AND c.parentId IS NULL AND c.deletedAt IS NULL ORDER BY c.createdAt ASC")
    Slice<Comment> findTopLevelByIssueId(UUID issueId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.parentId = :parentId AND c.deletedAt IS NULL ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentId(UUID parentId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.issueId = :issueId AND c.deletedAt IS NULL")
    long countByIssueId(UUID issueId);
}
