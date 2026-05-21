package com.projectmgmt.domain.workflow;

import com.projectmgmt.common.util.UUIDv7;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStatus {

    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    @Builder.Default
    private UUID id = UUIDv7.generate();

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusCategory category = StatusCategory.TODO;

    @Column(nullable = false)
    @Builder.Default
    private Integer position = 0;

    @Column(length = 7)
    @Builder.Default
    private String color = "#6B7280";

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
