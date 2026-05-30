# Phase 4 Testing Strategy — Kafka Outbox Publisher

## Goal

This document defines the testing and verification strategy for Phase 4 Kafka publishing from the outbox table.

## What Can Be Unit Tested

Unit tests should verify behavior without requiring a real Kafka broker.

Recommended test classes:

```text
KafkaTopicResolverTest
OutboxPublisherTest
```

## Unit Test Plan

### Kafka topic resolution

Test class:

```text
KafkaTopicResolverTest
```

Test cases:

```text
shouldResolveReservationCreatedTopic
shouldRejectUnsupportedEventType
```

Purpose:

- verify `ReservationCreatedEvent` maps to `ticket.reservation.created`
- reject unknown event types safely

### Outbox publisher success

Test class:

```text
OutboxPublisherTest
```

Test case:

```text
shouldPublishOutboxEventAndMarkAsPublished
```

Expected:

- `KafkaTemplate.send()` is called
- message key is aggregate id
- message value is JSON payload
- `OutboxService.markAsPublished()` is called

### Outbox publisher failure

Test case:

```text
shouldKeepOutboxEventPendingWhenKafkaPublishFails
```

Expected:

- `KafkaTemplate.send()` is attempted
- event is not lost
- `OutboxService.markPublishAttemptFailed()` is called
- event remains retryable

## What Should Be Manually Verified

Manual verification is required because local Kafka behavior should be checked end-to-end.

Manual checks:

- Docker Compose starts Kafka and Kafka UI
- Kafka topic exists
- reservation request creates outbox event
- scheduler publishes outbox event
- database status becomes `PUBLISHED`
- Kafka topic receives the JSON payload
- idempotency retry does not create duplicate outbox events

## Manual Verification Commands

Check infrastructure:

```powershell
docker compose -f infra/docker-compose.yml ps
```

Check topic:

```powershell
docker exec -it ticketflow-kafka kafka-topics.sh --bootstrap-server localhost:29092 --list
```

Read topic messages:

```powershell
docker exec -it ticketflow-kafka kafka-console-consumer.sh --bootstrap-server localhost:29092 --topic ticket.reservation.created --from-beginning --max-messages 5
```

Check outbox event:

```sql
select
    id,
    aggregate_type,
    aggregate_id,
    event_type,
    status,
    retry_count,
    error_message,
    created_at,
    published_at
from outbox_events
where aggregate_id = '<reservation_id>';
```

Check duplicate outbox event count:

```sql
select count(*) as outbox_event_count
from outbox_events
where aggregate_id = '<reservation_id>';
```

Expected:

```text
outbox_event_count = 1
```

## Kafka UI Screenshots To Capture

Capture:

- Dashboard showing `ticketflow-local` online
- Topics list showing `ticket.reservation.created`
- Messages page showing `ReservationCreatedEvent` payload

## Future Testcontainers Plan

Later, add integration tests with:

```text
PostgreSQL Testcontainer
Kafka Testcontainer
SpringBootTest
```

Suggested future test classes:

```text
OutboxKafkaIntegrationTest
ReservationOutboxKafkaIntegrationTest
```

Future integration test cases:

```text
shouldPublishPendingOutboxEventToKafka
shouldMarkOutboxEventAsPublishedAfterKafkaAck
shouldKeepOutboxEventPendingWhenKafkaIsUnavailable
shouldNotCreateDuplicateOutboxEventForIdempotentRetry
```

## Phase 4 Testing Checklist

- [x] unit tests pass
- [x] topic resolver test exists
- [x] outbox publisher success test exists
- [x] outbox publisher failure test exists
- [x] Kafka container verified
- [x] Kafka UI verified
- [x] topic creation verified
- [x] reservation creation verified
- [x] outbox `PUBLISHED` status verified
- [x] Kafka console consumer verified
- [x] idempotent retry count verified
- [ ] future Testcontainers integration tests
