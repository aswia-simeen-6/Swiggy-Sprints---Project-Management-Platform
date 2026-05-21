package com.projectmgmt.domain.comment;

import com.projectmgmt.domain.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.array.UUIDArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.UUID;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment extends BaseEntity {

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Type(UUIDArrayType.class)
    @Column(name = "mentions", columnDefinition = "uuid[]")
    @Builder.Default
    private UUID[] mentions = new UUID[]{};
}
