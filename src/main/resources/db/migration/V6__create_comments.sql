-- V6: Comments (threaded)
CREATE TABLE comments (
    id              UUID PRIMARY KEY,
    issue_id        UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    author_id       UUID NOT NULL REFERENCES users(id),
    parent_id       UUID REFERENCES comments(id) ON DELETE CASCADE,
    body            TEXT NOT NULL,
    mentions        UUID[] DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_comments_issue ON comments(issue_id, created_at ASC) WHERE deleted_at IS NULL;
CREATE INDEX idx_comments_parent ON comments(parent_id) WHERE parent_id IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_comments_author ON comments(author_id) WHERE deleted_at IS NULL;
