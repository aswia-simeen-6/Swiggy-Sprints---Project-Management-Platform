package com.projectmgmt.domain.sprint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, UUID> {

    Optional<Sprint> findByIdAndDeletedAtIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sprint s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<Sprint> findByIdForUpdate(UUID id);

    @Query("SELECT s FROM Sprint s WHERE s.projectId = :projectId AND s.deletedAt IS NULL ORDER BY s.createdAt DESC")
    List<Sprint> findByProjectId(UUID projectId);

    @Query("SELECT s FROM Sprint s WHERE s.projectId = :projectId AND s.status = 'ACTIVE' AND s.deletedAt IS NULL")
    Optional<Sprint> findActiveSprintByProjectId(UUID projectId);

    @Query("SELECT s FROM Sprint s WHERE s.projectId = :projectId AND s.status = 'COMPLETED' AND s.deletedAt IS NULL ORDER BY s.completedAt DESC")
    List<Sprint> findCompletedSprintsByProjectId(UUID projectId);

    @Query("SELECT COUNT(s) > 0 FROM Sprint s WHERE s.projectId = :projectId AND s.status = 'ACTIVE' AND s.deletedAt IS NULL")
    boolean hasActiveSprint(UUID projectId);
}
