-- V2: Projects and project members
CREATE TABLE projects (
    id              UUID PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    key             VARCHAR(10) NOT NULL,
    description     TEXT,
    owner_id        UUID NOT NULL REFERENCES users(id),
    issue_counter   INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_projects_key UNIQUE (key)
);

CREATE INDEX idx_projects_owner ON projects(owner_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_projects_key ON projects(key) WHERE deleted_at IS NULL;

CREATE TABLE project_members (
    id              UUID PRIMARY KEY,
    project_id      UUID NOT NULL REFERENCES projects(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    role            VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_project_member UNIQUE (project_id, user_id)
);

CREATE INDEX idx_pm_project ON project_members(project_id);
CREATE INDEX idx_pm_user ON project_members(user_id);
