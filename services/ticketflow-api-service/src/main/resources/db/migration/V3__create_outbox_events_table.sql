CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(30) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_outbox_events_status
        CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),

    CONSTRAINT chk_outbox_events_retry_count
        CHECK (retry_count >= 0)
);

CREATE INDEX idx_outbox_events_status_created_at
    ON outbox_events (status, created_at);

CREATE INDEX idx_outbox_events_aggregate
    ON outbox_events (aggregate_type, aggregate_id);

CREATE INDEX idx_outbox_events_event_type
    ON outbox_events (event_type);