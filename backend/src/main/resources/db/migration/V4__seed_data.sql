INSERT INTO users (id, name, email, password, created_at)
VALUES
    (
        '11111111-1111-1111-1111-111111111111',
        'Test User',
        'test@example.com',
        '$2a$12$BMj0sr.93Ffdtgx76v6LFOlvAx.NWmmIOm/iXnLpw/vdvQcQI8Yz.',
        CURRENT_TIMESTAMP
    ),
    (
        '22222222-2222-2222-2222-222222222222',
        'Assigned User',
        'assignee@example.com',
        '$2a$12$BMj0sr.93Ffdtgx76v6LFOlvAx.NWmmIOm/iXnLpw/vdvQcQI8Yz.',
        CURRENT_TIMESTAMP
    );

INSERT INTO projects (id, name, description, owner_id, created_at)
VALUES
    (
        '33333333-3333-3333-3333-333333333333',
        'Seed Project',
        'Project created by the seed migration for local testing.',
        '11111111-1111-1111-1111-111111111111',
        CURRENT_TIMESTAMP
    );

INSERT INTO tasks (
    id,
    title,
    description,
    status,
    priority,
    assignee_id,
    project_id,
    created_by,
    due_date,
    created_at
)
VALUES
    (
        '44444444-4444-4444-4444-444444444444',
        'Review product brief',
        'Seed task in TODO status.',
        'TODO',
        'HIGH',
        '11111111-1111-1111-1111-111111111111',
        '33333333-3333-3333-3333-333333333333',
        '11111111-1111-1111-1111-111111111111',
        '2026-04-15 09:00:00',
        CURRENT_TIMESTAMP
    ),
    (
        '55555555-5555-5555-5555-555555555555',
        'Build task API',
        'Seed task in IN_PROGRESS status.',
        'IN_PROGRESS',
        'MEDIUM',
        '22222222-2222-2222-2222-222222222222',
        '33333333-3333-3333-3333-333333333333',
        '11111111-1111-1111-1111-111111111111',
        '2026-04-17 09:00:00',
        CURRENT_TIMESTAMP
    ),
    (
        '66666666-6666-6666-6666-666666666666',
        'Document API examples',
        'Seed task in DONE status.',
        'DONE',
        'LOW',
        NULL,
        '33333333-3333-3333-3333-333333333333',
        '11111111-1111-1111-1111-111111111111',
        '2026-04-13 09:00:00',
        CURRENT_TIMESTAMP
    );
