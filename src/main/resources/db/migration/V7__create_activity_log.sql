-- V7: Activity log (append-only, immutable audit trail)
CREATE TABLE activity_log (
    id              UUID PRIMARY KEY,
    project_id      UUID NOT NULL REFERENCES projects(id),
    issue_id        UUID REFERENCES issues(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    action          VARCHAR(50) NOT NULL,
    changes         JSONB DEFAULT '{}',
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
    -- NOTE: No updated_at, no deleted_at, no version.
    -- This table is APPEND-ONLY by design. No updates or deletes ever.
);

CREATE INDEX idx_activity_project ON activity_log(project_id, created_at DESC);
CREATE INDEX idx_activity_issue ON activity_log(issue_id, created_at DESC) WHERE issue_id IS NOT NULL;
CREATE INDEX idx_activity_user ON activity_log(user_id, created_at DESC);
CREATE INDEX idx_activity_action ON activity_log(project_id, action, created_at DESC);

-- Prevent updates and deletes on activity_log via trigger
CREATE OR REPLACE FUNCTION prevent_activity_mutation() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Activity log is immutable. Updates and deletes are not allowed.';
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_activity_update
    BEFORE UPDATE ON activity_log
    FOR EACH ROW EXECUTE FUNCTION prevent_activity_mutation();

CREATE TRIGGER trg_prevent_activity_delete
    BEFORE DELETE ON activity_log
    FOR EACH ROW EXECUTE FUNCTION prevent_activity_mutation();
