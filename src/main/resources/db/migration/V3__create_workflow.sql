-- V3: Workflow statuses, transitions, conditions, and actions
CREATE TABLE workflow_statuses (
    id              UUID PRIMARY KEY,
    project_id      UUID NOT NULL REFERENCES projects(id),
    name            VARCHAR(50) NOT NULL,
    category        VARCHAR(20) NOT NULL DEFAULT 'TODO',
    position        INTEGER NOT NULL DEFAULT 0,
    color           VARCHAR(7) DEFAULT '#6B7280',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT uq_workflow_status_name UNIQUE (project_id, name),
    CONSTRAINT chk_category CHECK (category IN ('TODO', 'IN_PROGRESS', 'DONE'))
);

CREATE INDEX idx_ws_project ON workflow_statuses(project_id, position) WHERE deleted_at IS NULL;

CREATE TABLE workflow_transitions (
    id              UUID PRIMARY KEY,
    project_id      UUID NOT NULL REFERENCES projects(id),
    from_status_id  UUID NOT NULL REFERENCES workflow_statuses(id),
    to_status_id    UUID NOT NULL REFERENCES workflow_statuses(id),
    name            VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_transition UNIQUE (project_id, from_status_id, to_status_id),
    CONSTRAINT chk_different_status CHECK (from_status_id != to_status_id)
);

CREATE INDEX idx_wt_from ON workflow_transitions(from_status_id);
CREATE INDEX idx_wt_project ON workflow_transitions(project_id);

CREATE TABLE transition_conditions (
    id              UUID PRIMARY KEY,
    transition_id   UUID NOT NULL REFERENCES workflow_transitions(id) ON DELETE CASCADE,
    condition_type  VARCHAR(50) NOT NULL,
    config          JSONB NOT NULL DEFAULT '{}',
    position        INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tc_transition ON transition_conditions(transition_id, position);

CREATE TABLE transition_actions (
    id              UUID PRIMARY KEY,
    transition_id   UUID NOT NULL REFERENCES workflow_transitions(id) ON DELETE CASCADE,
    action_type     VARCHAR(50) NOT NULL,
    config          JSONB NOT NULL DEFAULT '{}',
    position        INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ta_transition ON transition_actions(transition_id, position);
