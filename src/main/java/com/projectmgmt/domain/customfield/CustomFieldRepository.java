package com.projectmgmt.domain.customfield;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomFieldRepository extends JpaRepository<CustomFieldDefinition, UUID> {

    @Query("SELECT cfd FROM CustomFieldDefinition cfd WHERE cfd.projectId = :projectId AND cfd.deletedAt IS NULL ORDER BY cfd.position ASC")
    List<CustomFieldDefinition> findByProjectId(UUID projectId);

    boolean existsByProjectIdAndFieldKeyAndDeletedAtIsNull(UUID projectId, String fieldKey);
}
