package com.projectmgmt.domain.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectmgmt.common.util.UUIDv7;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transition_conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransitionCondition {

    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    @Builder.Default
    private UUID id = UUIDv7.generate();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transition_id", nullable = false)
    private WorkflowTransition transition;

    @Column(name = "condition_type", nullable = false, length = 50)
    private String conditionType;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private JsonNode config = null;

    @Column(nullable = false)
    @Builder.Default
    private Integer position = 0;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
