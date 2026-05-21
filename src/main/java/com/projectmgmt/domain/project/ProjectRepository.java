package com.projectmgmt.domain.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByIdAndDeletedAtIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Project> findByIdForUpdate(UUID id);

    List<Project> findAllByDeletedAtIsNull();

    boolean existsByKeyAndDeletedAtIsNull(String key);

    @Query("SELECT p FROM Project p JOIN ProjectMember pm ON pm.project.id = p.id " +
           "WHERE pm.userId = :userId AND p.deletedAt IS NULL")
    List<Project> findAllByMemberUserId(UUID userId);
}
