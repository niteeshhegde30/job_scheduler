CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id),
    schedule_type VARCHAR NOT NULL,
    scheduled_at TIMESTAMP,
    cron_expression VARCHAR,
    parameters JSONB,
    status VARCHAR NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
