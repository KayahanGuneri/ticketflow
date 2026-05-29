# Phase 2 Testing Strategy: Reservation Transaction and Idempotency

## Purpose

This document defines the testing strategy for Phase 2 Backend: Reservation Transaction, Idempotency, and Optimistic Locking.

The goal is to verify that reservation creation is safe, repeatable, and consistent under normal, duplicate, invalid, and concurrent request scenarios.

## What We Test

Phase 2 tests focus on:

- Reservation creation
- Ticket stock decrease
- Duplicate `Idempotency-Key` handling
- Insufficient stock handling
- Missing `Idempotency-Key`
- Different request with same `Idempotency-Key`
- HTTP status behavior
- Domain-level stock consistency
- Optimistic locking concept

## Test Class List

Current test classes:

```text
TicketInventoryTest
ReservationServiceTest
ReservationControllerTest
```

Useful future integration test classes:

```text
ReservationFlowIntegrationTest
ReservationConcurrencyIntegrationTest
ReservationRepositoryIntegrationTest
```

## Unit Tests

Unit tests should verify business logic without starting the full Spring application.

### TicketInventoryTest

Purpose:

```text
Verify stock behavior at domain/entity level.
```

Test cases:

```text
shouldReserveTicketsWhenCapacityIsAvailable
shouldRejectReservationWhenCapacityIsInsufficient
shouldRejectReservationWhenTicketCountIsNotPositive
```

Why this matters:

`TicketInventory` owns the stock mutation rule. The service should not manually manipulate stock fields everywhere.

## Service Tests

### ReservationServiceTest

Purpose:

```text
Verify reservation use-case orchestration.
```

Test cases:

```text
shouldCreateReservationWhenStockIsAvailable
shouldReturnExistingReservationWhenIdempotencyKeyIsReusedWithSameRequest
shouldRejectIdempotencyKeyReuseWhenRequestIsDifferent
shouldRejectReservationWhenStockIsInsufficient
shouldRejectReservationWhenIdempotencyKeyIsBlank
```

What these tests protect:

- Duplicate requests do not decrease stock twice
- Same idempotency key with different request is rejected
- Insufficient stock does not create reservation
- Missing idempotency key does not reach persistence layer
- Service keeps transaction-related business flow clean

## Controller Tests

### ReservationControllerTest

Purpose:

```text
Verify HTTP-level behavior.
```

Test cases:

```text
shouldReturnCreatedWhenReservationIsNew
shouldReturnOkWhenReservationIsIdempotentRetry
```

What these tests protect:

- New reservation returns `201 Created`
- Idempotent retry returns `200 OK`
- Response body contains reservation information
- `Location` header is returned for new reservations

## Manual API Tests

### 1. Reservation Creation

Create an event with capacity 5.

Create a reservation with ticket count 2.

Expected result:

```text
HTTP 201 Created
Reservation status = PENDING
availableCapacity = 3
reservedCapacity = 2
```

### 2. Duplicate Idempotency-Key

Send the same request with the same `Idempotency-Key`.

Expected result:

```text
HTTP 200 OK
Same reservation id is returned
availableCapacity remains 3
reservedCapacity remains 2
```

### 3. Same Idempotency-Key With Different Request

Send the same `Idempotency-Key` with a different `ticketCount`.

Expected result:

```text
HTTP 409 Conflict
Stock does not change
```

### 4. Insufficient Stock

Request more tickets than available.

Expected result:

```text
HTTP 409 Conflict
Reservation is not created
Stock does not change
```

### 5. Missing Idempotency-Key

Send reservation request without the `Idempotency-Key` header.

Expected result:

```text
HTTP 400 Bad Request
Reservation is not created
Stock does not change
```

## Optimistic Locking Conceptual Test

Optimistic locking protects `TicketInventory` with a `version` field.

Conceptual scenario:

1. Request A loads ticket inventory with version 0
2. Request B loads the same ticket inventory with version 0
3. Request A reserves tickets and commits
4. Database increments version to 1
5. Request B tries to commit stale version 0
6. JPA detects conflict
7. API returns `409 Conflict`

The important guarantee:

```text
Stock must never become negative.
Successful reservations must never exceed total capacity.
```

## Manual Concurrency Test Scenario

Goal:

```text
Simulate multiple users trying to reserve tickets for the same event at the same time.
```

Setup:

```text
event totalCapacity = 3
parallel request count = 8
ticketCount per request = 1
unique Idempotency-Key per request
```

Expected result:

```text
Successful reservation count must not exceed 3
Final availableCapacity must never be negative
Final reservedCapacity must never exceed 3
Some requests may return 409 Conflict
```

Important note:

With optimistic locking and no retry policy, some requests may fail with conflict even if capacity appears to remain after the first conflict wave. This is acceptable for Phase 2 because this phase focuses on consistency, not automatic retry optimization.

## Unit Test vs Integration Test

### Unit Tests

Use unit tests for:

- Stock behavior
- Idempotency decision logic
- Service orchestration
- Controller HTTP status mapping

Current examples:

```text
TicketInventoryTest
ReservationServiceTest
ReservationControllerTest
```

### Integration Tests

Use integration tests for:

- Flyway migration verification
- Real PostgreSQL constraints
- Real JPA optimistic locking
- Actual transaction rollback behavior
- Repository query correctness
- Full API flow with MockMvc and database

Useful future test:

```text
ReservationFlowIntegrationTest
```

## Useful Testcontainers Tests Later

Later, Testcontainers can be used for:

```text
PostgreSQL reservation flow test
PostgreSQL optimistic locking test
Kafka outbox publishing test
Redis idempotency/cache test if Redis is introduced
Payment simulator integration test
```

Recommended future test classes:

```text
ReservationPostgresIntegrationTest
ReservationConcurrencyIntegrationTest
OutboxPostgresIntegrationTest
KafkaOutboxPublisherIntegrationTest
PaymentResultConsumerIntegrationTest
```

## Edge Cases

Important edge cases:

- `ticketCount = 0`
- `ticketCount < 0`
- Missing `eventId`
- Missing `userId`
- Missing `Idempotency-Key`
- Blank `Idempotency-Key`
- Very long `Idempotency-Key`
- Non-existing event id
- Event exists but ticket inventory is missing
- Same idempotency key with same request
- Same idempotency key with different request
- Requesting exactly remaining capacity
- Requesting more than remaining capacity
- Concurrent reservation requests

## Example Test Names

Recommended naming style:

```text
shouldCreateReservationWhenStockIsAvailable
shouldReturnExistingReservationWhenIdempotencyKeyIsReusedWithSameRequest
shouldRejectIdempotencyKeyReuseWhenRequestIsDifferent
shouldRejectReservationWhenStockIsInsufficient
shouldRejectReservationWhenIdempotencyKeyIsBlank
shouldReturnCreatedWhenReservationIsNew
shouldReturnOkWhenReservationIsIdempotentRetry
shouldNotOversellTicketsWhenConcurrentRequestsArrive
shouldRollbackReservationWhenStockUpdateFails
```

## What Output Should Be Sent Back For Review

After running tests, send:

```text
mvn test output
Manual reservation creation response
Duplicate idempotency response
Stock response after duplicate request
Insufficient stock response
Missing Idempotency-Key response
Concurrency script output if used
git status
```

## Current Phase 2 Test Result

Current result:

```text
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Phase 2 Testing Checklist

- Reservation domain behavior tested
- Reservation service behavior tested
- Reservation controller status behavior tested
- Duplicate idempotency behavior tested
- Insufficient stock behavior tested
- Missing idempotency key behavior tested
- Manual API tests completed
- `201 Created` verified for new reservation
- `200 OK` verified for idempotent retry
- `409 Conflict` verified for insufficient stock
- `400 Bad Request` verified for missing idempotency key
- Concurrency scenario documented
- Future Testcontainers plan documented
