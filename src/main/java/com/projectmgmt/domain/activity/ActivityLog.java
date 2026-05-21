package com.projectmgmt.domain.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectmgmt.common.util.UUIDv7;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activity_log")
@Immutable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    @Builder.Default
    private UUID id = UUIDv7.generate();

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(name = "issue_id", updatable = false)
    private UUID issueId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 50)
    private ActivityType action;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", updatable = false)
    @Builder.Default
    private JsonNode changes = null;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", updatable = false)
    @Builder.Default
    private JsonNode metadata = null;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
