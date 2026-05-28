# Testing Strategy

## Purpose

This document defines the testing strategy for TicketFlow.

The goal is to test important business behavior, consistency rules, and event-driven flows without overengineering the test suite.

## Testing Goals

TicketFlow tests must verify:

- event creation
- ticket inventory consistency
- reservation creation
- insufficient stock handling
- Idempotency-Key behavior
- optimistic locking behavior
- outbox event persistence
- Kafka publishing behavior
- payment result processing
- duplicate event handling
- stock restoration after payment failure

## Test Pyramid

### 1. Unit Tests

Purpose:

Test isolated business logic.

Tools:

- JUnit
- Mockito

Good candidates:

- ReservationService
- EventService
- OutboxService
- payment simulation logic
- status transition rules

### 2. Integration Tests

Purpose:

Test Spring context, repositories, database behavior, and transaction rules.

Tools:

- Spring Boot Test
- Testcontainers
- PostgreSQL container

Good candidates:

- repository tests
- reservation transaction tests
- idempotency constraint tests
- outbox persistence tests

### 3. Messaging Integration Tests

Purpose:

Test Kafka producer and consumer behavior where realistic.

Tools:

- Spring Boot Test
- Testcontainers Kafka

Good candidates:

- outbox publisher sends ReservationCreatedEvent
- payment result consumer handles PaymentCompletedEvent
- duplicate event is ignored using processed_events

## Important Test Cases

Event tests:

- shouldCreateEventWhenRequestIsValid
- shouldRejectEventWhenNameIsBlank
- shouldRejectEventWhenStartDateIsInPast

Reservation tests:

- shouldCreateReservationWhenStockIsAvailable
- shouldRejectReservationWhenStockIsInsufficient
- shouldReturnExistingReservationWhenIdempotencyKeyIsReused
- shouldRejectReservationWhenIdempotencyKeyIsMissing
- shouldPersistOutboxEventWhenReservationIsCreated

Payment tests:

- shouldConfirmReservationWhenPaymentCompleted
- shouldCancelReservationWhenPaymentFailed
- shouldRestoreStockWhenPaymentFailed
- shouldIgnoreDuplicatePaymentEvent

Outbox tests:

- shouldPublishPendingOutboxEvent
- shouldMarkOutboxEventAsPublishedAfterSuccessfulPublish
- shouldIncreaseRetryCountWhenPublishFails
- shouldMarkOutboxEventAsFailedWhenRetryLimitExceeded

## Test Naming Rule

Test methods should explain behavior clearly.

Format:

- shouldDoSomethingWhenConditionIsMet

Examples:

- shouldCreateEventWhenRequestIsValid
- shouldRejectReservationWhenStockIsInsufficient
- shouldReturnExistingReservationWhenIdempotencyKeyIsReused
- shouldIgnoreDuplicatePaymentEvent

## Arrange Act Assert

Tests should follow the Arrange Act Assert structure.

Arrange:

- prepare data
- configure mocks
- create request objects

Act:

- call the method or endpoint

Assert:

- verify result
- verify state changes
- verify interactions if needed

## What Not To Over-Test

Do not over-test:

- Lombok-generated methods
- trivial getters and setters
- framework behavior
- simple DTO constructors
- implementation details that do not affect business behavior

## Manual Verification

Some behavior can also be verified manually during early development.

Manual verification examples:

- check Kafka topics in Kafka UI
- check outbox_events table after reservation creation
- check processed_events table after payment result consumption
- check reservation status after payment simulation
- check ticket inventory after payment failure

## Testcontainers Plan

Use Testcontainers when infrastructure behavior matters.

Recommended containers:

- PostgreSQL
- Kafka
- Redis only if Redis behavior is used in the feature

Do not use Testcontainers for every small unit test.

## Interview Explanation

In TicketFlow, I would use unit tests for business logic, integration tests for database and transaction behavior, and Testcontainers for PostgreSQL and Kafka when infrastructure behavior matters. The most important tests are reservation consistency, idempotency key behavior, outbox persistence, and idempotent payment event consumption.
