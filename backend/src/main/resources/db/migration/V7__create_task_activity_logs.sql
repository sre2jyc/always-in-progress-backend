CREATE TABLE task_activity_logs (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id          UUID,
    project_id       UUID REFERENCES projects(id) ON DELETE SET NULL,
    actor_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    actor_type       VARCHAR CHECK (actor_type IN ('USER', 'AI_AGENT', 'SYSTEM')),
    action_type      VARCHAR CHECK (action_type IN ('CREATED', 'UPDATED', 'DELETED', 'STATUS_CHANGED', 'ASSIGNED')),
    old_value        JSONB,
    new_value        JSONB,
    changed_fields   TEXT[],
    conversation_id  UUID,
    metadata         JSONB,
    created_at       TIMESTAMP DEFAULT now()
);

CREATE INDEX ON task_activity_logs (task_id);
CREATE INDEX ON task_activity_logs (project_id);
CREATE INDEX ON task_activity_logs (actor_id);
CREATE INDEX ON task_activity_logs (created_at);
