package com.projectmgmt.domain.workflow;

import com.projectmgmt.common.util.UUIDv7;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "workflow_transitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTransition {

    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    @Builder.Default
    private UUID id = UUIDv7.generate();

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "from_status_id", nullable = false)
    private UUID fromStatusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_status_id", insertable = false, updatable = false)
    private WorkflowStatus fromStatus;

    @Column(name = "to_status_id", nullable = false)
    private UUID toStatusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_status_id", insertable = false, updatable = false)
    private WorkflowStatus toStatus;

    @Column(length = 100)
    private String name;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "transition", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    @Builder.Default
    private Set<TransitionCondition> conditions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "transition", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    @Builder.Default
    private Set<TransitionAction> actions = new LinkedHashSet<>();
}
