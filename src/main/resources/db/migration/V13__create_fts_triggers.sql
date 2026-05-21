-- V10: Full-text search trigger on issues
CREATE OR REPLACE FUNCTION issues_search_vector_trigger() RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.issue_key, '')), 'A');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_issues_search_vector
    BEFORE INSERT OR UPDATE OF title, description, issue_key ON issues
    FOR EACH ROW EXECUTE FUNCTION issues_search_vector_trigger();

-- Update existing rows (if any)
UPDATE issues SET search_vector =
    setweight(to_tsvector('english', COALESCE(title, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(description, '')), 'B') ||
    setweight(to_tsvector('english', COALESCE(issue_key, '')), 'A');
