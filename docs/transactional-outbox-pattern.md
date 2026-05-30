# Transactional Outbox Pattern

## Purpose

The Transactional Outbox Pattern is used to persist domain events together with business data in the same database transaction.

In TicketFlow, when a reservation is created, the system also creates a ReservationCreatedEvent. Instead of publishing directly to Kafka inside the reservation transaction, the event is first stored in the outbox_events table.

## Why Direct Kafka Publishing Inside a Database Transaction Is Risky

Database transactions and Kafka publishing are different consistency boundaries.

Risky scenarios:

- The reservation is committed, but Kafka publishing fails.
- Kafka publishing succeeds, but the database transaction rolls back.
- The application crashes between database commit and Kafka publish.
- Network issues create an uncertain Kafka publish result.

This can leave the system inconsistent.

Example:

    Reservation saved in database
    Kafka publish failed
    Payment simulator never receives ReservationCreatedEvent

The reservation exists, but the asynchronous workflow does not continue.

## What Transactional Outbox Solves

The outbox table makes event persistence atomic with business data persistence.

In TicketFlow, these operations happen in the same database transaction:

- reservations insert
- ticket_inventories update
- outbox_events insert

If the transaction commits, both the reservation and the outbox event exist.
If the transaction rolls back, neither the reservation nor the outbox event is persisted.

## Current Flow

    POST /api/v1/reservations
            |
            v
    ReservationService.createReservation()
            |
            |-- check idempotency key
            |-- load event
            |-- load ticket inventory
            |-- reserve ticket stock
            |-- save reservation
            |-- save ReservationCreatedEvent into outbox_events
            |
            v
    Database commit

## Outbox Table

Table name:

    outbox_events

Important columns:

| Column | Purpose |
|---|---|
| id | Unique outbox event id |
| aggregate_type | Domain aggregate type, for example RESERVATION |
| aggregate_id | Business aggregate id, for example reservation id |
| event_type | Event name, for example ReservationCreatedEvent |
| payload | JSON event payload |
| status | PENDING, PUBLISHED, FAILED |
| retry_count | Number of publish attempts |
| error_message | Last publish error |
| created_at | Event creation time |
| published_at | Successful publish time |

## ReservationCreatedEvent Payload

Current payload structure:

    {
      "messageId": "uuid",
      "eventType": "ReservationCreatedEvent",
      "reservationId": "uuid",
      "ticketEventId": "uuid",
      "userId": "uuid",
      "ticketCount": 2,
      "occurredAt": "timestamp"
    }

## At-Least-Once Delivery

The outbox publisher will eventually publish PENDING events to Kafka.

This pattern usually provides at-least-once delivery, not exactly-once delivery.

That means consumers must be idempotent. A consumer must safely handle duplicate events by checking whether the event was already processed.

## Current Phase Scope

This phase added:

- outbox_events table
- OutboxEvent entity
- OutboxStatus enum
- OutboxRepository
- OutboxService
- ReservationCreatedEvent
- scheduled publisher skeleton
- tests for outbox persistence during reservation creation

Real Kafka publishing will be added in a later phase.

## Why Outbox Logic Is Separated From Reservation Logic

ReservationService owns the reservation business use case.

OutboxService owns event persistence.

This separation keeps the design aligned with the Single Responsibility Principle.

ReservationService should not know Kafka publishing details. It only asks the outbox layer to persist an event record.

## Verification Queries

List outbox events:

    select
        id,
        aggregate_type,
        aggregate_id,
        event_type,
        status,
        retry_count,
        created_at,
        published_at
    from outbox_events
    order by created_at desc;

Check event payload:

    select payload
    from outbox_events
    where aggregate_id = '<reservation_id>';

Check idempotency behavior:

    select count(*) as outbox_event_count
    from outbox_events
    where aggregate_id = '<reservation_id>';

Expected result for an idempotent retry:

    outbox_event_count = 1

## Production Considerations

Future improvements:

- publish PENDING events to Kafka
- mark events as PUBLISHED after successful publish
- retry failed events
- move permanently failed events to a dead-letter flow
- use idempotent consumers
- add monitoring for stuck PENDING or FAILED events
- add outbox visibility API for dashboard
