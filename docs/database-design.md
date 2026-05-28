# Database Design

## Purpose

This document defines the initial PostgreSQL database design for TicketFlow.

PostgreSQL is the main source of truth for the system.

The database design must support reservation consistency, idempotency, optimistic locking, transactional outbox, and idempotent event consumption.

## General Database Rules

- Use UUID primary keys.
- Use created_at and updated_at consistently.
- Use version fields where optimistic locking is needed.
- Use database constraints for critical consistency rules.
- Do not rely only on application-level validation.
- Do not expose database entities directly from REST APIs.

## Tables

## 1. events

Purpose:

Stores ticketed events.

Columns:

- id UUID PRIMARY KEY
- name VARCHAR NOT NULL
- description TEXT
- location VARCHAR
- starts_at TIMESTAMP NOT NULL
- status VARCHAR NOT NULL
- created_at TIMESTAMP NOT NULL
- updated_at TIMESTAMP NOT NULL

Initial statuses:

- DRAFT
- ACTIVE
- CANCELLED

Recommended indexes:

- idx_events_status
- idx_events_starts_at

## 2. ticket_inventories

Purpose:

Stores ticket stock information for each event.

Columns:

- id UUID PRIMARY KEY
- event_id UUID NOT NULL
- total_capacity INTEGER NOT NULL
- available_capacity INTEGER NOT NULL
- reserved_capacity INTEGER NOT NULL
- version BIGINT NOT NULL
- created_at TIMESTAMP NOT NULL
- updated_at TIMESTAMP NOT NULL

Constraints:

- event_id must reference events(id)
- total_capacity must be greater than or equal to 0
- available_capacity must be greater than or equal to 0
- reserved_capacity must be greater than or equal to 0
- available_capacity + reserved_capacity must not exceed total_capacity
- event_id should be unique if each event has only one inventory record

Recommended indexes:

- idx_ticket_inventories_event_id

Concurrency rule:

The version field will be used for optimistic locking during reservation creation and stock restoration.

## 3. reservations

Purpose:

Stores ticket reservation records.

Columns:

- id UUID PRIMARY KEY
- event_id UUID NOT NULL
- user_id UUID NOT NULL
- ticket_count INTEGER NOT NULL
- status VARCHAR NOT NULL
- idempotency_key VARCHAR NOT NULL
- created_at TIMESTAMP NOT NULL
- updated_at TIMESTAMP NOT NULL

Initial statuses:

- PENDING
- CONFIRMED
- CANCELLED
- EXPIRED

Constraints:

- event_id must reference events(id)
- ticket_count must be greater than 0
- idempotency_key must be unique

Recommended indexes:

- idx_reservations_event_id
- idx_reservations_user_id
- idx_reservations_status
- uq_reservations_idempotency_key

Idempotency rule:

If the same Idempotency-Key is reused, the system should return the existing reservation instead of creating a duplicate reservation.

## 4. payments

Purpose:

Stores payment result information related to reservations.

Columns:

- id UUID PRIMARY KEY
- reservation_id UUID NOT NULL
- status VARCHAR NOT NULL
- amount NUMERIC
- failure_reason TEXT
- created_at TIMESTAMP NOT NULL
- updated_at TIMESTAMP NOT NULL

Initial statuses:

- REQUESTED
- COMPLETED
- FAILED

Constraints:

- reservation_id must reference reservations(id)
- reservation_id should be unique if one reservation has one payment record

Recommended indexes:

- idx_payments_reservation_id
- idx_payments_status

## 5. outbox_events

Purpose:

Stores domain events before they are published to Kafka.

Columns:

- id UUID PRIMARY KEY
- aggregate_type VARCHAR NOT NULL
- aggregate_id UUID NOT NULL
- event_type VARCHAR NOT NULL
- payload JSONB NOT NULL
- status VARCHAR NOT NULL
- retry_count INTEGER NOT NULL
- error_message TEXT
- created_at TIMESTAMP NOT NULL
- published_at TIMESTAMP

Initial statuses:

- PENDING
- PUBLISHED
- FAILED

Recommended indexes:

- idx_outbox_events_status
- idx_outbox_events_created_at
- idx_outbox_events_aggregate_id
- idx_outbox_events_event_type

Outbox rule:

Outbox events must be persisted in the same transaction as the business state change.

## 6. processed_events

Purpose:

Tracks consumed Kafka events to make consumers idempotent.

Columns:

- id UUID PRIMARY KEY
- event_id UUID NOT NULL
- event_type VARCHAR NOT NULL
- consumer_name VARCHAR NOT NULL
- processed_at TIMESTAMP NOT NULL

Constraints:

- event_id and consumer_name should be unique together

Recommended indexes:

- idx_processed_events_event_id
- idx_processed_events_consumer_name
- uq_processed_events_event_id_consumer_name

Consumer idempotency rule:

Before applying a consumed Kafka event, the consumer must check processed_events.

If the event was already processed by the same consumer, the event must be ignored safely.

## Important Consistency Rules

### Reservation Creation

Reservation creation must happen in one transaction:

1. Validate event.
2. Validate inventory.
3. Decrease available_capacity.
4. Increase reserved_capacity.
5. Create reservation with PENDING status.
6. Persist ReservationCreatedEvent in outbox_events.

### Payment Completed

Payment completed processing must happen in one transaction:

1. Check processed_events.
2. Mark reservation as CONFIRMED.
3. Save payment as COMPLETED.
4. Save processed event record.

### Payment Failed

Payment failed processing must happen in one transaction:

1. Check processed_events.
2. Mark reservation as CANCELLED.
3. Restore ticket inventory.
4. Save payment as FAILED.
5. Save processed event record.

## Future Migration Tool

Flyway can be used for database migrations.

Initial migration idea:

- V1__create_events_table.sql
- V2__create_ticket_inventories_table.sql
- V3__create_reservations_table.sql
- V4__create_payments_table.sql
- V5__create_outbox_events_table.sql
- V6__create_processed_events_table.sql

## Interview Explanation

In TicketFlow, PostgreSQL is the source of truth. I use UUID primary keys, constraints, indexes, optimistic locking for stock consistency, a unique idempotency key for duplicate request protection, an outbox_events table for reliable Kafka publishing, and a processed_events table for idempotent consumers.
