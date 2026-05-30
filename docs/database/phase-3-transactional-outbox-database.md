# Phase 3 Database Design — Transactional Outbox

## Goal

This document explains the database design of Phase 3 Transactional Outbox Pattern in TicketFlow.

The goal of this phase is to persist domain events safely together with reservation data in the same database transaction.

When a reservation is created, the system writes:

- reservation data into `reservations`
- stock changes into `ticket_inventories`
- event data into `outbox_events`

These writes must succeed or fail together.

## Why Transactional Outbox Needs a Database Table

Directly publishing to Kafka inside the reservation transaction is risky because the database transaction and Kafka publish operation are not part of the same atomic transaction.

Possible failure scenarios:

- reservation is committed but Kafka publish fails
- Kafka publish succeeds but database transaction rolls back
- application crashes between database commit and Kafka publish
- network timeout creates an uncertain publish result

The `outbox_events` table solves this by storing the event first inside the database transaction. A separate publisher later reads `PENDING` events and publishes them to Kafka.

## `outbox_events` Table Design

Table name:

```text
outbox_events
```

Current table structure:

| Column | Type | Nullable | Purpose |
|---|---:|---:|---|
| `id` | `UUID` | No | Unique identifier of the outbox event |
| `aggregate_type` | `VARCHAR(80)` | No | Domain aggregate type, for example `RESERVATION` |
| `aggregate_id` | `UUID` | No | Business aggregate id, for example reservation id |
| `event_type` | `VARCHAR(120)` | No | Event name, for example `ReservationCreatedEvent` |
| `payload` | `JSONB` | No | Serialized event payload |
| `status` | `VARCHAR(30)` | No | Current event status: `PENDING`, `PUBLISHED`, `FAILED` |
| `retry_count` | `INTEGER` | No | Number of publishing attempts |
| `error_message` | `VARCHAR(1000)` | Yes | Last publishing error message |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | No | Event creation timestamp |
| `published_at` | `TIMESTAMP WITH TIME ZONE` | Yes | Successful publish timestamp |

## Column Explanations

### `id`

`id` is the unique identifier of the outbox record.

It is different from the domain aggregate id.

Example:

```text
outbox_events.id = 3ed8ec09-b4aa-41db-95a9-1e860a03dba0
outbox_events.aggregate_id = ca7e0098-4fc8-4661-948f-2f46c0ceef43
```

The first one identifies the outbox record.
The second one identifies the reservation.

### `aggregate_type`

`aggregate_type` tells which domain aggregate produced the event.

Example:

```text
RESERVATION
```

This makes the outbox table reusable for future aggregates such as payment or order.

### `aggregate_id`

`aggregate_id` stores the id of the related business entity.

For `ReservationCreatedEvent`, this is the reservation id.

This allows easy tracing from event to business data.

### `event_type`

`event_type` identifies the kind of event.

Example:

```text
ReservationCreatedEvent
```

Future examples:

```text
PaymentCompletedEvent
PaymentFailedEvent
ReservationConfirmedEvent
ReservationCancelledEvent
```

Consumers and publishers can use this field to route or serialize events correctly.

### `payload`

`payload` stores the serialized event body.

Current `ReservationCreatedEvent` payload:

```json
{
  "messageId": "uuid",
  "eventType": "ReservationCreatedEvent",
  "reservationId": "uuid",
  "ticketEventId": "uuid",
  "userId": "uuid",
  "ticketCount": 2,
  "occurredAt": "timestamp"
}
```

## Why `payload` Is Stored As JSONB

`payload` is stored as `JSONB` because event payloads can evolve independently.

Benefits:

- event structure can be explicit
- no need to create a separate table for every event type
- PostgreSQL can validate JSON format
- payload can still be inspected during debugging
- future queries can read JSON fields if needed

Alternative options:

| Option | Pros | Cons |
|---|---|---|
| `TEXT` | Simple, database-agnostic | No JSON validation |
| `JSONB` | Queryable and validated by PostgreSQL | PostgreSQL-specific |
| Separate event tables | Strong schema per event | Too much complexity for MVP |

For TicketFlow MVP, `JSONB` is a good balance.

## Why `status` Is Useful

`status` controls the lifecycle of the outbox event.

Allowed values:

```text
PENDING
PUBLISHED
FAILED
```

Meaning:

| Status | Meaning |
|---|---|
| `PENDING` | Event is waiting to be published |
| `PUBLISHED` | Event was successfully published |
| `FAILED` | Event publishing failed after retry logic or due to an error |

The scheduler reads `PENDING` events.

## Why `retry_count` Is Useful

`retry_count` tracks how many times the system attempted to publish the event.

It helps with:

- retry control
- debugging
- detecting poison messages
- deciding when to stop retrying
- moving events to a dead-letter flow later

Example:

```text
retry_count = 0
```

means the event has not been attempted yet.

## Why `error_message` Is Useful

`error_message` stores the latest publish failure reason.

Example:

```text
Kafka timeout while publishing to ticket.reservation.created
```

This helps operators understand why an event is stuck or failed.

The message should be useful, but not too large and not sensitive.

## Why `created_at` Is Useful

`created_at` shows when the event was created.

It is important for:

- ordering
- debugging
- monitoring old pending events
- selecting events in FIFO-like order

The publisher should generally process older events first.

## Why `published_at` Is Useful

`published_at` shows when the event was successfully published.

It helps measure:

- event publishing latency
- time spent in `PENDING`
- operational health of the outbox publisher

If `published_at` is `null`, the event is not published yet.

## Indexes

Current indexes:

```sql
CREATE INDEX idx_outbox_events_status_created_at
    ON outbox_events (status, created_at);

CREATE INDEX idx_outbox_events_aggregate
    ON outbox_events (aggregate_type, aggregate_id);

CREATE INDEX idx_outbox_events_event_type
    ON outbox_events (event_type);
```

## Indexing Strategy For `PENDING` Events

The most important query is:

```sql
select *
from outbox_events
where status = 'PENDING'
order by created_at asc
limit 20;
```

That is why this index is important:

```sql
idx_outbox_events_status_created_at
```

It supports:

- filtering by `status`
- ordering by `created_at`
- efficient batch publishing

## Query Methods

Current repository methods:

```java
List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

long countByStatus(OutboxStatus status);
```

These methods are enough for the current phase.

## JPA Entity Mapping Plan

Entity:

```text
OutboxEvent
```

Enum:

```text
OutboxStatus
```

Repository:

```text
OutboxRepository
```

Important mapping decisions:

| Java Field | Database Column |
|---|---|
| `id` | `id` |
| `aggregateType` | `aggregate_type` |
| `aggregateId` | `aggregate_id` |
| `eventType` | `event_type` |
| `payload` | `payload` |
| `status` | `status` |
| `retryCount` | `retry_count` |
| `errorMessage` | `error_message` |
| `createdAt` | `created_at` |
| `publishedAt` | `published_at` |

`status` should be mapped with:

```java
@Enumerated(EnumType.STRING)
```

This keeps the database readable and avoids ordinal enum problems.

`payload` should be mapped as JSON:

```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "payload", nullable = false, columnDefinition = "jsonb")
```

## Database Risks

### Risk 1: Outbox table grows forever

If published events are never archived or deleted, the table can grow indefinitely.

Possible future solutions:

- archive old `PUBLISHED` events
- delete old `PUBLISHED` events after retention period
- partition by date if needed

For MVP, no cleanup is necessary yet.

### Risk 2: Too many `PENDING` events

If Kafka is down, `PENDING` events can accumulate.

Future monitoring should track:

```sql
select count(*)
from outbox_events
where status = 'PENDING';
```

### Risk 3: Duplicate publishing

Transactional Outbox usually gives at-least-once delivery.

That means the same event can be published more than once in failure/retry scenarios.

Consumers must be idempotent.

### Risk 4: Large payloads

Storing huge payloads can make the table heavy.

The event payload should contain only fields needed by consumers.

Do not serialize entire JPA entities.

### Risk 5: Missing index for publisher query

Without an index on `(status, created_at)`, polling `PENDING` events can become slow.

## Avoiding Overengineering

For this phase, the design intentionally avoids:

- generic event framework
- separate outbox table per event type
- complex retry state machine
- dead-letter table
- exactly-once delivery claims
- Kafka transaction integration
- payload schema registry

The current design is enough for MVP:

```text
save business data + save event data in one database transaction
```

Kafka publishing and retry sophistication will be added later.

## Database Checklist

- [x] `outbox_events` table exists
- [x] `payload` is stored as `JSONB`
- [x] `status` supports `PENDING`, `PUBLISHED`, `FAILED`
- [x] `retry_count` starts from `0`
- [x] `created_at` is saved when event is created
- [x] `published_at` is nullable
- [x] index exists for `(status, created_at)`
- [x] aggregate lookup index exists
- [x] event type index exists
- [x] JPA entity maps table correctly
- [x] reservation creation writes outbox event
- [x] idempotent retry does not create duplicate outbox event
