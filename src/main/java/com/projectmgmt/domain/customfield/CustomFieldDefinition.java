package com.projectmgmt.domain.customfield;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectmgmt.common.util.UUIDv7;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "custom_field_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomFieldDefinition {

    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    @Builder.Default
    private UUID id = UUIDv7.generate();

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "field_key", nullable = false, length = 50)
    private String fieldKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 20)
    private CustomFieldType fieldType;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private JsonNode options = null;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private boolean required = false;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(nullable = false)
    @Builder.Default
    private Integer position = 0;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
