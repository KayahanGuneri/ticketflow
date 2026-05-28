CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    location VARCHAR(255) NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_events_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'CANCELLED'))
);

CREATE TABLE ticket_inventories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL UNIQUE,
    total_capacity INTEGER NOT NULL,
    available_capacity INTEGER NOT NULL,
    reserved_capacity INTEGER NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_ticket_inventories_event
        FOREIGN KEY (event_id)
        REFERENCES events (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_ticket_inventories_total_capacity
        CHECK (total_capacity > 0),

    CONSTRAINT chk_ticket_inventories_available_capacity
        CHECK (available_capacity >= 0),

    CONSTRAINT chk_ticket_inventories_reserved_capacity
        CHECK (reserved_capacity >= 0),

    CONSTRAINT chk_ticket_inventories_capacity_consistency
        CHECK (available_capacity + reserved_capacity = total_capacity)
);

CREATE INDEX idx_events_status ON events (status);
CREATE INDEX idx_events_starts_at ON events (starts_at);
CREATE INDEX idx_ticket_inventories_event_id ON ticket_inventories (event_id);