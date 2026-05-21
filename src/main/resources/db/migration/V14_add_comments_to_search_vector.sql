-- V14: Include comment body text in issue search_vector

-- Helper function to aggregate all comments for an issue into a tsvector
CREATE OR REPLACE FUNCTION issue_comments_tsvector(issue_uuid UUID) RETURNS tsvector AS $$
    SELECT COALESCE(
        setweight(to_tsvector('english', string_agg(COALESCE(body, ''), ' ')), 'C'),
        ''::tsvector
    )
    FROM comments
    WHERE issue_id = issue_uuid AND deleted_at IS NULL;
$$ LANGUAGE sql STABLE;

-- Updated trigger function that includes comments
CREATE OR REPLACE FUNCTION issues_search_vector_trigger() RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.issue_key, '')), 'A') ||
        issue_comments_tsvector(NEW.id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger on comments table: when a comment is added/updated/deleted, refresh the parent issue's search_vector
CREATE OR REPLACE FUNCTION comments_search_vector_trigger() RETURNS TRIGGER AS $$
DECLARE
    target_issue_id UUID;
BEGIN
    IF TG_OP = 'DELETE' THEN
        target_issue_id := OLD.issue_id;
    ELSE
        target_issue_id := NEW.issue_id;
    END IF;

    -- Touch the issue to re-fire its search_vector trigger
    UPDATE issues SET
        search_vector =
            setweight(to_tsvector('english', COALESCE(title, '')), 'A') ||
            setweight(to_tsvector('english', COALESCE(description, '')), 'B') ||
            setweight(to_tsvector('english', COALESCE(issue_key, '')), 'A') ||
            issue_comments_tsvector(target_issue_id)
    WHERE id = target_issue_id;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_comments_search_vector
    AFTER INSERT OR UPDATE OF body OR DELETE ON comments
    FOR EACH ROW EXECUTE FUNCTION comments_search_vector_trigger();

-- Backfill existing issues to include their comments in the search vector
UPDATE issues SET search_vector =
    setweight(to_tsvector('english', COALESCE(title, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(description, '')), 'B') ||
    setweight(to_tsvector('english', COALESCE(issue_key, '')), 'A') ||
    issue_comments_tsvector(id);