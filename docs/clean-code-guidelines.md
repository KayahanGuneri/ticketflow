# Clean Code Guidelines

## Purpose

This document defines the Clean Code rules for TicketFlow.

The goal is to keep the project readable, testable, and professional.

Clean code is more important than clever code.

## Naming Rules

Use meaningful names.

Good examples:

- ReservationService
- TicketInventoryRepository
- CreateReservationRequest
- ReservationResponse
- OutboxEventPublisher
- PaymentResultConsumer

Bad examples:

- Manager
- Processor
- HandlerService
- DataObject
- TempService
- doStuff

Names should explain intent.

## Method Rules

Methods should be small and focused.

A method should usually do one thing clearly.

Prefer guard clauses over deeply nested if blocks.

Bad direction:

- one huge createReservation method with validation, stock update, outbox creation, and response mapping mixed together without structure

Good direction:

- validateRequest
- findActiveEvent
- reserveTicketCapacity
- createPendingReservation
- persistReservationCreatedOutboxEvent
- buildReservationResponse

## Class Responsibility Rules

Each class should have one clear reason to change.

Examples:

- ReservationController changes when HTTP contract changes
- ReservationService changes when reservation business rules change
- ReservationRepository changes when reservation persistence changes
- OutboxPublisher changes when Kafka publishing changes
- GlobalExceptionHandler changes when error response format changes

## Controller Rules

Controllers must be thin.

Controllers should:

- receive requests
- validate request DTOs
- call services
- return response DTOs

Controllers should not:

- update ticket capacity directly
- create outbox events directly
- publish Kafka messages directly
- contain transaction logic
- contain complex business rules

## DTO and Entity Rules

Do not expose JPA entities directly from REST APIs.

Use DTOs for request and response models.

Reasons:

- API contracts stay stable
- database structure stays internal
- validation becomes clearer
- accidental data exposure is reduced

Required DTO examples:

- CreateEventRequest
- EventResponse
- CreateReservationRequest
- ReservationResponse
- DashboardSummaryResponse

## Validation Rules

Never trust client input.

Use validation annotations where appropriate:

- @NotNull
- @NotBlank
- @Positive
- @Future
- @Min
- @Max

Validate:

- event name
- event date
- event capacity
- reservation ticket count
- Idempotency-Key header
- UUID values
- state transitions

## Error Handling Rules

Use consistent error responses.

Standard error response should include:

- timestamp
- status
- error
- message
- path
- traceId if useful later

Do not leak internal exceptions to API clients.

Do not return stack traces in API responses.

Use meaningful custom exceptions:

- ResourceNotFoundException
- BusinessRuleViolationException
- InsufficientTicketCapacityException
- DuplicateRequestException
- ReservationConflictException
- OutboxPublishingException

## Transaction Rules

Use @Transactional at the service layer.

Do not put transaction boundaries in controllers.

Important transaction rules:

- reservation creation and stock decrease must be atomic
- reservation creation and outbox persistence must be atomic
- payment result processing must be atomic
- stock restoration after payment failure must be safe
- consumer idempotency check and state update must be in the same transaction

## Kafka and Outbox Rules

Do not publish JPA entities as Kafka payloads.

Use explicit event DTOs or records.

Every event should include:

- eventId
- eventType
- aggregateId
- occurredAt
- required payload fields

Consumers must be idempotent.

Assume at-least-once delivery.

Duplicate events must not corrupt data.

## Comment Rules

All code comments must be written in English.

Do not add comments for obvious code.

Bad comment example:

// increment count

Good comment example:

// We persist the outbox event in the same transaction to avoid losing domain events after reservation creation.

Use comments only for:

- non-obvious design decisions
- transaction boundaries
- concurrency risks
- idempotency behavior
- outbox consistency behavior
- Kafka retry and DLQ decisions
- production risks
- TODO items

## Logging Rules

Use meaningful logs.

Do not log sensitive data.

Do not log entire payloads unnecessarily.

Good log events:

- reservation created
- outbox event persisted
- outbox event published
- payment result consumed
- duplicate event ignored
- Kafka publish failed
- DLQ message produced

## Testing Clean Code Rules

Test names should explain behavior.

Good test method names:

- shouldCreateEventWhenRequestIsValid
- shouldRejectReservationWhenStockIsInsufficient
- shouldReturnExistingReservationWhenIdempotencyKeyIsReused
- shouldPersistOutboxEventWhenReservationIsCreated
- shouldIgnoreDuplicatePaymentEvent

Tests should follow Arrange Act Assert structure.

Tests should verify business behavior, not implementation details.

## Refactoring Rules

Refactor when:

- a method becomes too long
- logic is duplicated
- names do not explain intent
- controller contains business rules
- service becomes a God class
- tests are hard to write because responsibilities are mixed

Do not refactor only to add unnecessary patterns.

## Interview Explanation

In an interview, explain this simply:

In TicketFlow, I follow Clean Code by keeping controllers thin, business logic in services, persistence in repositories, and API contracts in DTOs. I avoid exposing entities directly, I use meaningful names, I keep transaction boundaries at the service layer, and I add comments only when they explain non-obvious technical decisions such as outbox, idempotency, or concurrency behavior.
