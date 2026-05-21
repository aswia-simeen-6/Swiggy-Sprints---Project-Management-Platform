-- V5: Issues
CREATE TABLE issues (
    id              UUID PRIMARY KEY,
    project_id      UUID NOT NULL REFERENCES projects(id),
    issue_key       VARCHAR(20) NOT NULL,
    issue_type      VARCHAR(20) NOT NULL,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    status_id       UUID NOT NULL REFERENCES workflow_statuses(id),
    priority        VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    assignee_id     UUID REFERENCES users(id),
    reporter_id     UUID NOT NULL REFERENCES users(id),
    sprint_id       UUID REFERENCES sprints(id),
    parent_id       UUID REFERENCES issues(id),
    story_points    INTEGER,
    labels          TEXT[] DEFAULT '{}',
    custom_fields   JSONB DEFAULT '{}',
    search_vector   TSVECTOR,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT uq_issue_key UNIQUE (issue_key),
    CONSTRAINT chk_issue_type CHECK (issue_type IN ('EPIC', 'STORY', 'TASK', 'BUG', 'SUBTASK')),
    CONSTRAINT chk_priority CHECK (priority IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'LOWEST')),
    CONSTRAINT chk_story_points CHECK (story_points IS NULL OR story_points >= 0),
    CONSTRAINT chk_subtask_parent CHECK (
        (issue_type = 'SUBTASK' AND parent_id IS NOT NULL) OR
        (issue_type != 'SUBTASK')
    )
);

-- Partial indexes — only active records
CREATE INDEX idx_issues_project_status ON issues(project_id, status_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_issues_sprint ON issues(sprint_id) WHERE deleted_at IS NULL AND sprint_id IS NOT NULL;
CREATE INDEX idx_issues_assignee ON issues(assignee_id) WHERE deleted_at IS NULL AND assignee_id IS NOT NULL;
CREATE INDEX idx_issues_reporter ON issues(reporter_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_issues_parent ON issues(parent_id) WHERE parent_id IS NOT NULL;
CREATE INDEX idx_issues_type ON issues(project_id, issue_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_issues_priority ON issues(project_id, priority) WHERE deleted_at IS NULL;
CREATE INDEX idx_issues_created ON issues(project_id, created_at DESC) WHERE deleted_at IS NULL;

-- Full-text search
CREATE INDEX idx_issues_search ON issues USING GIN(search_vector);

-- Custom fields
CREATE INDEX idx_issues_custom ON issues USING GIN(custom_fields jsonb_path_ops);

-- Labels array
CREATE INDEX idx_issues_labels ON issues USING GIN(labels);

-- Issue watchers
CREATE TABLE issue_watchers (
    id              UUID PRIMARY KEY,
    issue_id        UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_issue_watcher UNIQUE (issue_id, user_id)
);

CREATE INDEX idx_iw_issue ON issue_watchers(issue_id);
CREATE INDEX idx_iw_user ON issue_watchers(user_id);
