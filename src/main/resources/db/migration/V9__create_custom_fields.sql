-- V9: Custom field definitions per project
CREATE TABLE custom_field_definitions (
    id              UUID PRIMARY KEY,
    project_id      UUID NOT NULL REFERENCES projects(id),
    name            VARCHAR(100) NOT NULL,
    field_key       VARCHAR(50) NOT NULL,
    field_type      VARCHAR(20) NOT NULL,
    options         JSONB DEFAULT '[]',
    is_required     BOOLEAN NOT NULL DEFAULT FALSE,
    default_value   VARCHAR(500),
    position        INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT uq_custom_field UNIQUE (project_id, field_key),
    CONSTRAINT chk_field_type CHECK (field_type IN ('TEXT', 'NUMBER', 'DROPDOWN', 'DATE', 'CHECKBOX', 'URL'))
);

CREATE INDEX idx_cfd_project ON custom_field_definitions(project_id, position) WHERE deleted_at IS NULL;
