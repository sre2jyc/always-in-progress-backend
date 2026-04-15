CREATE TABLE outbox (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type    VARCHAR NOT NULL,
    payload       JSONB NOT NULL,
    processed     BOOLEAN DEFAULT false,
    created_at    TIMESTAMP DEFAULT now(),
    processed_at  TIMESTAMP
);

CREATE INDEX ON outbox (processed, created_at);
