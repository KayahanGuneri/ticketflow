# MVP Scope

## Purpose

This document defines the first realistic MVP scope for TicketFlow.

The goal is to build a backend-focused event-driven ticket reservation and payment orchestration platform without turning the project into an overcomplicated enterprise system.

The MVP must be advanced enough to demonstrate real backend engineering skills, but still realistic for a junior / new graduate backend developer portfolio project.

## MVP Goals

The MVP must demonstrate:

- ticket event management
- ticket inventory consistency
- reservation creation
- idempotent reservation requests
- transactional consistency with PostgreSQL
- optimistic locking for ticket stock
- transactional outbox pattern
- Kafka-based asynchronous communication
- payment simulation
- payment result processing
- eventual consistency
- idempotent Kafka consumers
- basic retry and DLQ concept
- dashboard visibility
- clean documentation
- testable backend design

## In Scope

### 1. Event Management

The system must support basic event management.

Required capabilities:

- create event
- list events
- get event details
- manage event status with simple states

Initial event statuses:

- DRAFT
- ACTIVE
- CANCELLED

### 2. Ticket Inventory Management

Each event must have ticket inventory.

Required capabilities:

- create initial ticket inventory for an event
- track total capacity
- track available capacity
- track reserved capacity
- protect stock consistency with optimistic locking

Important rule:

Ticket capacity changes must be handled transactionally.

### 3. Reservation Management

The system must support ticket reservation.

Required capabilities:

- create reservation
- validate requested ticket count
- check available capacity
- decrease available capacity
- increase reserved capacity
- store reservation status
- return reservation details

Initial reservation statuses:

- PENDING
- CONFIRMED
- CANCELLED
- EXPIRED

### 4. Idempotency-Key Support

Reservation creation must support an Idempotency-Key header.

Purpose:

- prevent duplicate reservation creation caused by client retries
- return the existing reservation if the same request is repeated with the same key
- avoid corrupting ticket inventory

Initial rule:

The idempotency key must be unique for reservation creation.

### 5. Transactional Outbox Pattern

Reservation creation must also create an outbox event in the same database transaction.

Purpose:

- prevent losing domain events
- avoid publishing Kafka events directly inside the main business transaction
- make event publishing retryable

Required outbox fields:

- id
- aggregate_type
- aggregate_id
- event_type
- payload
- status
- retry_count
- error_message
- created_at
- published_at

Initial outbox statuses:

- PENDING
- PUBLISHED
- FAILED

### 6. Kafka Event Publishing

The system must publish reservation events to Kafka from the outbox table.

Initial topic:

- ticket.reservation.created

Initial event:

- ReservationCreatedEvent

The event must include:

- eventId
- eventType
- aggregateId
- reservationId
- userId
- ticketCount
- occurredAt

### 7. Payment Simulator Service

The payment-simulator-service must consume ReservationCreatedEvent.

Its purpose is to simulate asynchronous payment processing.

Required behavior:

- consume reservation created event
- simulate success or failure
- publish payment result event

Initial payment topics:

- ticket.payment.completed
- ticket.payment.failed

Initial payment events:

- PaymentCompletedEvent
- PaymentFailedEvent

### 8. Payment Result Processing

ticketflow-api-service must consume payment result events.

Required behavior:

- if payment is completed, confirm reservation
- if payment fails, cancel reservation
- if payment fails, restore ticket stock safely
- ignore duplicate payment result events

### 9. Processed Event Tracking

The system must track processed Kafka events.

Purpose:

- make Kafka consumers idempotent
- avoid applying the same event twice
- protect reservation and stock state from duplicate event delivery

Required table:

- processed_events

Required fields:

- id
- event_id
- event_type
- consumer_name
- processed_at

### 10. Dashboard APIs

The backend must expose simple dashboard APIs.

Required endpoints:

- GET /api/v1/dashboard/summary
- GET /api/v1/dashboard/recent-reservations
- GET /api/v1/dashboard/recent-events

Purpose:

- make backend behavior visible
- support frontend monitoring
- help technical interview explanation

### 11. Frontend Dashboard

The frontend must be simple and backend-focused.

Required pages:

- DashboardPage
- EventsPage
- EventDetailPage
- ReservationsPage
- OutboxEventsPage

Frontend must include:

- loading states
- error states
- empty states
- status badges
- API client separation
- typed API responses

### 12. Docker Compose Local Infrastructure

The local infrastructure must include:

- PostgreSQL
- Kafka
- Kafka UI
- Redis if useful

Backend and frontend containerization can be added after the local development flow is stable.

### 13. Testing

The MVP must include meaningful tests.

Required test types:

- unit tests
- service layer tests
- repository/integration tests
- Testcontainers-based tests where realistic

Important test cases:

- shouldCreateEventWhenRequestIsValid
- shouldRejectReservationWhenStockIsInsufficient
- shouldReturnExistingReservationWhenIdempotencyKeyIsReused
- shouldPersistOutboxEventWhenReservationIsCreated
- shouldIgnoreDuplicatePaymentEvent

## Out of Scope Before MVP

The following items must not be implemented before the MVP is complete:

- Kubernetes
- real payment gateway integration
- full authentication system
- complex RBAC
- Prometheus
- Grafana
- cloud deployment
- multi-tenant SaaS design
- mobile app
- machine learning
- complex notification service
- unnecessary microservices
- overly complex frontend animations

## Optional After MVP

The following items may be considered after MVP completion:

- authentication
- basic authorization
- notification service
- observability improvements
- Prometheus and Grafana
- cloud deployment
- MCP or AI assistant tools
- admin dashboard improvements
- advanced retry and DLQ dashboard
- Go-based helper service

## Definition of Done

The MVP is complete when:

- an event can be created
- ticket inventory can be created
- a reservation can be created with Idempotency-Key
- ticket capacity is updated safely
- duplicate reservation requests do not create duplicate reservations
- ReservationCreatedEvent is persisted in outbox
- outbox publisher publishes the event to Kafka
- payment simulator consumes reservation event
- payment simulator publishes payment result event
- main API service consumes payment result event
- reservation is confirmed or cancelled correctly
- failed payment restores ticket stock
- duplicate payment events are ignored
- dashboard APIs show useful summary data
- frontend dashboard visualizes the main backend flow
- Docker Compose infrastructure works locally
- important flows have tests
- documentation explains the system clearly
