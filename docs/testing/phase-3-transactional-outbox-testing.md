# Phase 3 Testing Strategy — Transactional Outbox

## Goal

This document defines the testing strategy for Phase 3 Transactional Outbox Pattern in TicketFlow.

The main goal is to verify that reservation creation and outbox event persistence behave consistently.

## What Must Be Tested

When a reservation is created successfully:

- ticket inventory must be updated
- reservation must be saved
- `ReservationCreatedEvent` must be saved into `outbox_events`
- outbox event status must be `PENDING`
- retry count must start from `0`

When reservation creation fails:

- reservation must not be saved
- ticket inventory must not be incorrectly changed
- outbox event must not be created

## Atomic Persistence

Atomic persistence means:

```text
reservation + inventory update + outbox event
```

must be committed together.

If one part fails, the whole operation should roll back.

Expected behavior:

| Scenario | Reservation | Inventory | Outbox Event |
|---|---|---|---|
| Successful reservation | Saved | Updated | Saved |
| Event not found | Not saved | Not updated | Not saved |
| Missing inventory | Not saved | Not updated | Not saved |
| Insufficient stock | Not saved | Not updated | Not saved |
| Duplicate idempotency key with same request | Existing reservation returned | Not updated again | Not created again |
| Duplicate idempotency key with different request | Rejected | Not updated | Not created |

## Why Failed Business Operations Should Not Create Outbox Events

An outbox event represents a real domain fact.

Example:

```text
ReservationCreatedEvent
```

means:

```text
A reservation was actually created.
```

If reservation creation fails, publishing `ReservationCreatedEvent` would be incorrect.

Bad example:

```text
Stock insufficient
Reservation rejected
ReservationCreatedEvent still saved
```

This would cause downstream systems such as payment simulator to react to a reservation that does not exist.

Therefore, outbox events should be persisted only after successful domain state changes.

## Unit Test Boundaries

Unit tests should verify service orchestration and business rules without a real database.

Good unit test targets:

- `ReservationService`
- `OutboxService`
- `TicketInventory`

Current and suggested unit test classes:

```text
ReservationServiceTest
OutboxServiceTest
TicketInventoryTest
```

Unit tests should verify:

- reservation is created when stock is available
- stock is decreased correctly
- outbox service is called for successful reservation
- outbox service is not called for failed reservation
- duplicate idempotency retry does not call outbox service
- insufficient stock throws exception
- invalid idempotency key is rejected

## Integration Test Boundaries

Integration tests should verify real database behavior.

Suggested future integration test classes:

```text
ReservationOutboxIntegrationTest
OutboxRepositoryIntegrationTest
```

Integration tests should verify:

- Flyway creates `outbox_events`
- JPA can persist `OutboxEvent`
- `payload` is stored as JSONB
- reservation and outbox event are saved in the same transaction
- rollback removes both reservation and outbox event
- repository can find `PENDING` events ordered by `created_at`

For PostgreSQL-specific JSONB behavior, Testcontainers is preferred over H2.

## Manual Testing Boundaries

Manual tests are useful for verifying the full local runtime.

Manual verification should include:

- start PostgreSQL with Docker Compose
- start `ticketflow-api-service` with local profile
- create an event
- create a reservation
- check `outbox_events`
- retry the same request with the same `Idempotency-Key`
- verify only one outbox event exists
- verify scheduler logs `PENDING` event

## Test Class List

### Existing / Current

```text
ReservationServiceTest
ReservationControllerTest
TicketInventoryTest
EventServiceTest
EventControllerTest
TicketflowApiServiceApplicationTests
```

### Recommended For This Phase

```text
OutboxServiceTest
ReservationOutboxIntegrationTest
OutboxRepositoryIntegrationTest
```

## Test Cases

### Reservation success

Test name:

```text
shouldCreateReservationWhenStockIsAvailable
```

Expected:

- result is created
- reservation response contains correct event id
- inventory available capacity decreases
- inventory reserved capacity increases
- outbox event persistence is triggered

### Outbox event persistence

Test name:

```text
shouldPersistOutboxEventWhenReservationIsCreated
```

Expected:

- `OutboxService.saveReservationCreatedEvent()` is called
- event is created only after reservation save

### Idempotent retry

Test name:

```text
shouldReturnExistingReservationWhenIdempotencyKeyIsReusedWithSameRequest
```

Expected:

- existing reservation is returned
- inventory is not updated again
- reservation is not saved again
- outbox event is not created again

### Idempotency conflict

Test name:

```text
shouldRejectIdempotencyKeyReuseWhenRequestIsDifferent
```

Expected:

- `DuplicateRequestException` is thrown
- inventory is not updated
- outbox event is not created

### Insufficient stock

Test name:

```text
shouldRejectReservationWhenStockIsInsufficient
```

Expected:

- `InsufficientTicketCapacityException` is thrown
- reservation is not saved
- outbox event is not created

### Blank idempotency key

Test name:

```text
shouldRejectReservationWhenIdempotencyKeyIsBlank
```

Expected:

- `BusinessRuleViolationException` is thrown
- repository lookup is not performed
- outbox event is not created

### Pending event query

Test name:

```text
shouldFindPendingOutboxEventsOrderedByCreatedAt
```

Expected:

- only `PENDING` events are returned
- oldest events are returned first
- query respects batch size

### Event payload serialization

Test name:

```text
shouldSerializeReservationCreatedEventPayload
```

Expected:

- payload contains `messageId`
- payload contains `eventType`
- payload contains `reservationId`
- payload contains `ticketEventId`
- payload contains `userId`
- payload contains `ticketCount`
- payload contains `occurredAt`

## Edge Cases

Important edge cases:

- same `Idempotency-Key` with same request
- same `Idempotency-Key` with different request
- event id does not exist
- ticket inventory does not exist
- ticket count is greater than available stock
- idempotency key is blank
- idempotency key exceeds maximum length
- outbox payload serialization fails
- scheduler finds no pending events
- scheduler finds pending events but Kafka is not implemented yet

## What Output Should Be Sent For Review

During Phase 3 testing review, send:

```text
mvn test output
```

Expected:

```text
BUILD SUCCESS
```

Send manual API output:

```text
POST /api/v1/events response
POST /api/v1/reservations response
POST /api/v1/reservations retry response
```

Send SQL output:

```sql
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
```

Send payload output:

```sql
select payload
from outbox_events
where aggregate_id = '<reservation_id>';
```

Send idempotency count output:

```sql
select count(*) as outbox_event_count
from outbox_events
where aggregate_id = '<reservation_id>';
```

Send scheduler logs:

```text
Found 1 pending outbox event(s) ready for publishing
Outbox event ready: id=..., eventType=ReservationCreatedEvent, aggregateType=RESERVATION, aggregateId=...
```

## Phase 3 Testing Checklist

- [x] unit tests pass
- [x] reservation success test exists
- [x] insufficient stock test exists
- [x] idempotency retry test exists
- [x] idempotency conflict test exists
- [x] outbox service call is verified during successful reservation
- [x] outbox service is not called during failed reservation
- [x] manual reservation creation verified
- [x] manual outbox insert verified
- [x] manual idempotent retry verified
- [x] manual inventory update verified
- [x] scheduler reads `PENDING` events
- [ ] future integration test with Testcontainers verifies real transaction rollback
- [ ] future repository integration test verifies JSONB mapping
