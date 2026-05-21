package com.projectmgmt.domain.project;

import com.projectmgmt.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "issue_counter", nullable = false)
    @Builder.Default
    private Integer issueCounter = 0;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProjectMember> members = new ArrayList<>();

    public int nextIssueNumber() {
        this.issueCounter++;
        return this.issueCounter;
    }

    public String generateIssueKey() {
        return this.key + "-" + nextIssueNumber();
    }
}
