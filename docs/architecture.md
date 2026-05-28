# Architecture

## Purpose

This document describes the initial architecture of TicketFlow.

TicketFlow is an event-driven ticket reservation and payment orchestration platform.

The architecture is designed to demonstrate realistic backend engineering concepts such as transactional consistency, asynchronous communication, idempotency, outbox-based publishing, and eventual consistency.

## Architectural Style

TicketFlow uses a backend-focused full-stack monorepo architecture.

The system is not designed as a large enterprise microservice system in the first MVP.

Instead, it uses a pragmatic service-oriented structure with two backend services and one frontend dashboard.

## Main Components

### 1. ticketflow-api-service

ticketflow-api-service is the main backend service.

Responsibilities:

- event management
- ticket inventory management
- reservation creation
- reservation lifecycle management
- stock consistency
- Idempotency-Key handling
- transactional outbox persistence
- Kafka event publishing from outbox
- payment result consumption
- dashboard APIs
- outbox visibility
- processed event tracking

This service owns the main PostgreSQL database tables.

### 2. payment-simulator-service

payment-simulator-service simulates an external payment system.

Responsibilities:

- consume ReservationCreatedEvent
- simulate payment success or failure
- publish PaymentCompletedEvent
- publish PaymentFailedEvent

This service exists to demonstrate asynchronous communication between services.

It is intentionally simple and does not integrate with a real payment provider.

### 3. ticketflow-dashboard

ticketflow-dashboard is the frontend application.

Responsibilities:

- show dashboard summary
- list events
- show event details
- create reservations
- list reservations
- show reservation statuses
- show outbox events
- show recent event flow

The frontend is not the main focus of the project.

It exists to make backend behavior visible and explainable.

### 4. PostgreSQL

PostgreSQL is the main source of truth.

It stores:

- events
- ticket inventories
- reservations
- payments
- outbox events
- processed events

PostgreSQL is responsible for critical consistency guarantees.

Important consistency rules must not rely only on application-level checks.

### 5. Kafka

Kafka is used for asynchronous event-driven communication.

Initial topics:

- ticket.reservation.created
- ticket.payment.completed
- ticket.payment.failed
- ticket.reservation.confirmed
- ticket.reservation.cancelled
- ticket.dlq

Kafka delivery should be treated as at-least-once.

Because of this, consumers must be idempotent.

### 6. Redis

Redis may be used when it provides real value.

Possible use cases:

- temporary idempotency helper
- lightweight cache
- future dashboard optimization

For the early MVP, PostgreSQL-backed idempotency is acceptable because it is simpler and more reliable.

### 7. Kafka UI

Kafka UI is used for local development and debugging.

It helps inspect:

- topics
- messages
- consumer groups
- message flow

Kafka UI is not part of the business logic.

## Main Business Flow

### Reservation Flow

1. Client sends POST /api/v1/reservations with Idempotency-Key.
2. ticketflow-api-service validates the request.
3. ticketflow-api-service checks event and ticket inventory.
4. Available ticket capacity is decreased transactionally.
5. Reserved ticket capacity is increased transactionally.
6. Reservation is created with PENDING status.
7. ReservationCreatedEvent is saved into outbox_events in the same transaction.
8. Outbox publisher reads PENDING outbox events.
9. Outbox publisher publishes ReservationCreatedEvent to Kafka.
10. Outbox event status is updated to PUBLISHED.

### Payment Flow

1. payment-simulator-service consumes ReservationCreatedEvent.
2. It simulates payment success or failure.
3. It publishes PaymentCompletedEvent or PaymentFailedEvent.
4. ticketflow-api-service consumes the payment result event.
5. If payment succeeds, reservation becomes CONFIRMED.
6. If payment fails, reservation becomes CANCELLED.
7. If payment fails, ticket stock is restored safely.
8. The processed event is recorded in processed_events.

## Consistency Model

TicketFlow uses strong consistency inside a single service transaction and eventual consistency between services.

Strong consistency is required for:

- reservation creation
- ticket capacity update
- outbox event persistence
- payment result state transition
- stock restoration after payment failure

Eventual consistency is acceptable for:

- payment simulation result
- dashboard reflection delay
- Kafka-based communication between services

## Transaction Boundaries

Transaction boundaries must be placed at the service layer.

Important transaction rules:

- reservation creation and stock decrease must happen in the same transaction
- reservation creation and outbox event persistence must happen in the same transaction
- Kafka publishing must not happen directly inside the main reservation transaction
- payment result processing must be transactional
- duplicate event checks must be part of the consumer transaction

## Idempotency Strategy

Reservation creation must use Idempotency-Key.

The goal is to prevent duplicate reservation creation when clients retry the same request.

Initial strategy:

- require Idempotency-Key header for reservation creation
- store idempotency_key with reservation
- use a unique constraint to prevent duplicates
- return existing reservation when the same key is reused

## Outbox Strategy

The transactional outbox pattern is used to reliably publish domain events.

Instead of publishing Kafka messages directly during reservation creation, the service saves an outbox event in the same transaction.

A separate publisher process reads pending outbox events and publishes them to Kafka.

This reduces the risk of losing domain events after successful database commits.

## Consumer Idempotency Strategy

Kafka consumers must assume duplicate delivery.

Each consumed event must be checked against processed_events.

If an event was already processed, the consumer must ignore it safely.

This protects reservation and stock state from duplicate Kafka messages.

## Service Responsibility Rule

Controllers must not contain business logic.

Recommended responsibility split:

- controller: HTTP request and response handling
- service: business use cases
- repository: persistence
- entity: persistence model
- DTO: API contract
- publisher: Kafka publishing
- consumer: Kafka consuming
- mapper: DTO/entity conversion if needed
- exception handler: standard error responses

## Why This Architecture Is Realistic

TicketFlow is realistic because it models common backend problems:

- inventory consistency
- concurrent reservations
- duplicate client requests
- asynchronous payment processing
- reliable event publishing
- idempotent consumers
- failure handling
- dashboard visibility

These problems appear in real reservation, ticketing, ordering, and booking systems.

## Why This Is Not a Fraud Project

TicketFlow does not detect suspicious behavior.

It does not classify transactions.

It does not calculate fraud scores.

It does not focus on anomaly monitoring.

Its core problem is reservation and payment orchestration.
