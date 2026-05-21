package com.projectmgmt.domain.issue;

import com.projectmgmt.common.util.UUIDv7;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "issue_watchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueWatcher {

    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    @Builder.Default
    private UUID id = UUIDv7.generate();

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
