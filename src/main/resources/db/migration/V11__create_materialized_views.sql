-- V11: Materialized views for board state and sprint velocity

-- Board state: pre-computed column counts per project
CREATE MATERIALIZED VIEW mv_board_summary AS
SELECT
    p.id AS project_id,
    ws.id AS status_id,
    ws.name AS status_name,
    ws.category AS status_category,
    ws.position AS status_position,
    COUNT(i.id) AS issue_count,
    COALESCE(SUM(i.story_points), 0) AS total_story_points
FROM projects p
CROSS JOIN workflow_statuses ws
LEFT JOIN issues i ON i.status_id = ws.id AND i.project_id = p.id AND i.deleted_at IS NULL
WHERE ws.project_id = p.id AND ws.deleted_at IS NULL AND p.deleted_at IS NULL
GROUP BY p.id, ws.id, ws.name, ws.category, ws.position
ORDER BY p.id, ws.position;

CREATE UNIQUE INDEX idx_mv_board_summary ON mv_board_summary(project_id, status_id);

-- Sprint velocity history
CREATE MATERIALIZED VIEW mv_sprint_velocity AS
SELECT
    s.project_id,
    s.id AS sprint_id,
    s.name AS sprint_name,
    s.completed_at,
    s.velocity,
    s.completed_points,
    s.total_points,
    CASE WHEN s.total_points > 0
         THEN ROUND((s.completed_points::NUMERIC / s.total_points) * 100, 1)
         ELSE 0
    END AS completion_rate
FROM sprints s
WHERE s.status = 'COMPLETED' AND s.deleted_at IS NULL
ORDER BY s.project_id, s.completed_at DESC;

CREATE UNIQUE INDEX idx_mv_sprint_velocity ON mv_sprint_velocity(project_id, sprint_id);

-- Function to refresh materialized views
CREATE OR REPLACE FUNCTION refresh_board_summary() RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_board_summary;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION refresh_sprint_velocity() RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_sprint_velocity;
END;
$$ LANGUAGE plpgsql;
