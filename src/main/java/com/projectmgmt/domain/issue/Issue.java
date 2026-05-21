package com.projectmgmt.domain.issue;

import com.projectmgmt.domain.common.BaseEntity;
import com.projectmgmt.domain.workflow.WorkflowStatus;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Issue extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "issue_key", nullable = false, unique = true, length = 20)
    private String issueKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 20)
    private IssueType issueType;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "status_id", nullable = false)
    private UUID statusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", insertable = false, updatable = false)
    private WorkflowStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Column(name = "sprint_id")
    private UUID sprintId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "story_points")
    private Integer storyPoints;

    @Type(StringArrayType.class)
    @Column(name = "labels", columnDefinition = "text[]")
    @Builder.Default
    private String[] labels = new String[]{};

    @Type(JsonType.class)
    @Column(name = "custom_fields", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> customFields = Map.of();
}
