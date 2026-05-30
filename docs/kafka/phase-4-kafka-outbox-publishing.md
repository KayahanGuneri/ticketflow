# Phase 4 Kafka Publishing From Outbox

## Goal

Phase 4 publishes pending outbox events to Kafka.

In Phase 3, reservation creation persisted a `ReservationCreatedEvent` into the `outbox_events` table.

In Phase 4, the scheduled outbox publisher reads `PENDING` events, publishes them to Kafka, and updates the outbox event status.

## Target Topic

```text
ticket.reservation.created
```

## Runtime Flow

```text
POST /api/v1/reservations
        |
        v
ReservationService
        |
        |-- save reservation
        |-- update ticket inventory
        |-- save ReservationCreatedEvent into outbox_events
        |
        v
outbox_events.status = PENDING
        |
        v
OutboxPublisherScheduler
        |
        v
OutboxPublisher
        |
        v
Kafka topic: ticket.reservation.created
        |
        v
outbox_events.status = PUBLISHED
```

## Kafka Producer Basics

The producer sends messages to a Kafka topic.

In this phase:

- Kafka topic: `ticket.reservation.created`
- message key: reservation id / aggregate id
- message value: serialized `ReservationCreatedEvent` JSON payload

The message key is useful because events related to the same reservation can be routed consistently.

## Topic Naming

Topic names should be meaningful and stable.

Current topic:

```text
ticket.reservation.created
```

Naming structure:

```text
domain.aggregate.event
```

Meaning:

| Part | Meaning |
|---|---|
| `ticket` | business domain |
| `reservation` | aggregate/process |
| `created` | event action |

## Outbox Polling

`OutboxPublisherScheduler` periodically queries `PENDING` events.

Current polling properties:

```yaml
ticketflow:
  outbox:
    publisher:
      fixed-delay-ms: 5000
      batch-size: 20
      publish-timeout-seconds: 10
```

The scheduler should not contain Kafka infrastructure details. It only coordinates polling and delegates publishing to `OutboxPublisher`.

## Status Transitions

Successful publish:

```text
PENDING -> PUBLISHED
```

Updated fields:

| Field | Value |
|---|---|
| `status` | `PUBLISHED` |
| `published_at` | current timestamp |
| `error_message` | cleared |
| `retry_count` | unchanged |

Failed publish:

```text
PENDING -> PENDING
```

Updated fields:

| Field | Value |
|---|---|
| `status` | `PENDING` |
| `retry_count` | incremented |
| `error_message` | latest publish error |
| `published_at` | remains null |

## Why Failed Publishing Must Not Lose Events

Kafka publishing can fail because of:

- Kafka broker unavailable
- network issues
- timeout
- topic configuration problem
- serialization problem

If publishing fails, the event must not be deleted or marked as successfully published.

Keeping the event as `PENDING` allows the scheduler to retry later.

## Eventual Consistency

This design supports eventual consistency.

The reservation transaction commits first with the outbox event. Kafka publishing happens asynchronously after the database transaction.

This means downstream services may receive the event slightly later, but the event is not lost.

## Separation Of Concerns

Responsibilities:

| Class | Responsibility |
|---|---|
| `ReservationService` | reservation business use case |
| `OutboxService` | outbox persistence and status transitions |
| `OutboxPublisherScheduler` | polling pending events |
| `OutboxPublisher` | Kafka publishing |
| `KafkaTopicResolver` | event type to topic mapping |
| `KafkaTopicConfig` | topic creation |

Kafka logic must not be placed inside controllers or reservation domain logic.

## OOP/SOLID Notes

- Single Responsibility Principle: publishing, polling, and reservation logic are separated.
- Open/Closed Principle: new event types can be added through topic mapping.
- Dependency Inversion: reservation flow does not depend on Kafka infrastructure.
- Encapsulation: `OutboxEvent` owns its status transition methods.

## Verification Evidence

Expected successful log:

```text
Published outbox event to Kafka: id=..., topic=ticket.reservation.created, eventType=ReservationCreatedEvent, aggregateId=...
```

Expected database result:

```text
status = PUBLISHED
retry_count = 0
published_at is not null
```

Expected Kafka message:

```json
{
  "eventType": "ReservationCreatedEvent",
  "reservationId": "uuid",
  "ticketEventId": "uuid",
  "userId": "uuid",
  "ticketCount": 2
}
```
