-- V4: Sprints
CREATE TABLE sprints (
    id              UUID PRIMARY KEY,
    project_id      UUID NOT NULL REFERENCES projects(id),
    name            VARCHAR(200) NOT NULL,
    goal            TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    start_date      DATE,
    end_date        DATE,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    velocity        INTEGER,
    completed_points INTEGER,
    total_points    INTEGER,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_sprint_status CHECK (status IN ('PLANNED', 'ACTIVE', 'COMPLETED')),
    CONSTRAINT chk_sprint_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_sprints_project ON sprints(project_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_sprints_active ON sprints(project_id) WHERE status = 'ACTIVE' AND deleted_at IS NULL;
