-- V12: Seed a default project with standard workflow for development

-- Insert a system/seed user
INSERT INTO users (id, email, password_hash, display_name, role)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin@projectmgmt.dev',
    -- bcrypt hash of 'admin123'
    '$2a$12$LQv3c1yqBo9SkvXS7QTJOOeqZqGzHuYMJwvpOhi7o/0TYVNdNI6KO',
    'Admin User',
    'ADMIN'
);

INSERT INTO users (id, email, password_hash, display_name, role)
VALUES (
    'a0000000-0000-0000-0000-000000000002',
    'jane@projectmgmt.dev',
    '$2a$12$LQv3c1yqBo9SkvXS7QTJOOeqZqGzHuYMJwvpOhi7o/0TYVNdNI6KO',
    'Jane Smith',
    'MEMBER'
),
(
    'a0000000-0000-0000-0000-000000000003',
    'bob@projectmgmt.dev',
    '$2a$12$LQv3c1yqBo9SkvXS7QTJOOeqZqGzHuYMJwvpOhi7o/0TYVNdNI6KO',
    'Bob Chen',
    'MEMBER'
);

-- Create a sample project
INSERT INTO projects (id, name, key, description, owner_id, issue_counter)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'Project Alpha',
    'ALPHA',
    'A sample project for development and testing.',
    'a0000000-0000-0000-0000-000000000001',
    0
);

-- Add members
INSERT INTO project_members (id, project_id, user_id, role) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'ADMIN'),
    ('c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', 'MEMBER'),
    ('c0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000003', 'MEMBER');

-- Standard Kanban workflow statuses
INSERT INTO workflow_statuses (id, project_id, name, category, position, color) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'To Do',       'TODO',        0, '#6B7280'),
    ('d0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'In Progress', 'IN_PROGRESS', 1, '#3B82F6'),
    ('d0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001', 'In Review',   'IN_PROGRESS', 2, '#F59E0B'),
    ('d0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000001', 'Done',        'DONE',        3, '#10B981');

-- Allowed transitions (enforces workflow)
-- To Do → In Progress
INSERT INTO workflow_transitions (id, project_id, from_status_id, to_status_id, name) VALUES
    ('e0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001',
     'd0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002', 'Start Work');

-- In Progress → In Review
INSERT INTO workflow_transitions (id, project_id, from_status_id, to_status_id, name) VALUES
    ('e0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001',
     'd0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000003', 'Submit for Review');

-- In Review → Done
INSERT INTO workflow_transitions (id, project_id, from_status_id, to_status_id, name) VALUES
    ('e0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001',
     'd0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000004', 'Approve');

-- In Review → In Progress (send back)
INSERT INTO workflow_transitions (id, project_id, from_status_id, to_status_id, name) VALUES
    ('e0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000001',
     'd0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000002', 'Request Changes');

-- In Progress → To Do (de-prioritize)
INSERT INTO workflow_transitions (id, project_id, from_status_id, to_status_id, name) VALUES
    ('e0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000001',
     'd0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000001', 'Move to Backlog');

-- Transition condition: "Submit for Review" requires assignee to be set
INSERT INTO transition_conditions (id, transition_id, condition_type, config, position) VALUES
    ('f0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002',
     'FIELD_REQUIRED', '{"field": "assignee_id", "message": "An assignee is required before submitting for review"}', 0);

-- Transition action: "Approve" auto-sets a metadata field
INSERT INTO transition_actions (id, transition_id, action_type, config, position) VALUES
    ('f1000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003',
     'NOTIFY_WATCHERS', '{"message": "Issue has been approved and marked as Done"}', 0);

-- Create a sample sprint
INSERT INTO sprints (id, project_id, name, goal, status, start_date, end_date)
VALUES (
    'b1000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    'Sprint 1',
    'Set up project foundation and core features',
    'PLANNED',
    '2026-05-20',
    '2026-06-03'
);

-- Refresh materialized views
SELECT refresh_board_summary();
SELECT refresh_sprint_velocity();
