# Interview Notes

## Purpose

This document explains how to present Phase 0 of TicketFlow in a technical interview.

The goal is to explain the project clearly as a backend-focused full-stack portfolio project.

## 30-Second Project Explanation

TicketFlow is an event-driven ticket reservation and payment orchestration platform. The main goal is to demonstrate backend engineering concepts beyond CRUD, such as ticket inventory consistency, idempotent reservation requests, optimistic locking, transactional outbox, Kafka-based asynchronous payment simulation, and idempotent event consumers. The frontend exists mainly to visualize the backend flow.

## 60-Second Architecture Explanation

The system has two backend services and one frontend dashboard. ticketflow-api-service owns the main business flow: event management, ticket inventory, reservations, outbox, payment result consumption, and dashboard APIs. payment-simulator-service consumes reservation-created events from Kafka and publishes payment success or failure events. PostgreSQL is the source of truth, Kafka is used for asynchronous communication, Redis is available for helper use cases, and Kafka UI helps inspect event flow locally.

## Why This Project Is Advanced But Realistic

This project is advanced because it includes real backend problems:

- concurrent ticket reservation
- stock consistency
- duplicate client request handling
- transactional outbox
- asynchronous service communication
- event-driven payment result processing
- idempotent consumers
- retry and DLQ concept

It is realistic because these problems exist in ticketing, booking, ordering, and reservation systems.

## Difference From Fraud or Anomaly Projects

TicketFlow is not a fraud detection or anomaly monitoring project.

It does not detect suspicious behavior.

It does not calculate risk scores.

It does not monitor fraudulent transactions.

Its main focus is reservation and payment orchestration.

The core problems are:

- ticket capacity consistency
- reservation lifecycle
- payment result handling
- reliable event publishing
- idempotent message consumption

## How To Explain The Reservation Flow

1. The client sends a reservation request with an Idempotency-Key.
2. The API validates the request.
3. The service checks event and ticket inventory.
4. Ticket capacity is updated in PostgreSQL.
5. A PENDING reservation is created.
6. ReservationCreatedEvent is saved into outbox_events in the same transaction.
7. An outbox publisher later publishes the event to Kafka.
8. payment-simulator-service consumes the event.
9. It publishes payment success or failure.
10. The API service consumes the payment result and confirms or cancels the reservation.

## How To Explain Transactional Outbox

Transactional outbox prevents losing domain events.

Instead of publishing Kafka messages directly inside the reservation transaction, the system saves the event into the outbox_events table in the same transaction as the reservation.

After the transaction commits, a separate publisher reads pending outbox events and publishes them to Kafka.

This makes event publishing more reliable and retryable.

## How To Explain Idempotency-Key

Idempotency-Key protects the system from duplicate client retries.

If the client sends the same reservation request again with the same key, the system should return the existing reservation instead of creating a second reservation.

This is important because duplicate reservations could corrupt ticket stock.

## How To Explain Optimistic Locking

Optimistic locking protects ticket inventory during concurrent reservation attempts.

The ticket_inventories table has a version field.

If two users try to reserve the last tickets at the same time, optimistic locking helps prevent both transactions from incorrectly succeeding.

## How To Explain Kafka In This Project

Kafka decouples reservation creation from payment processing.

ticketflow-api-service publishes ReservationCreatedEvent.

payment-simulator-service consumes it and publishes PaymentCompletedEvent or PaymentFailedEvent.

ticketflow-api-service consumes the payment result and updates reservation status.

Because Kafka delivery can be at-least-once, consumers must be idempotent.

## How To Explain Processed Events

processed_events table is used for consumer idempotency.

Before processing a Kafka event, the consumer checks whether the event was already processed.

If it was already processed, the consumer ignores it safely.

This prevents duplicate Kafka messages from corrupting reservation or stock state.

## How To Explain Frontend Role

The frontend is not the main focus of this project.

It exists to visualize backend behavior.

It shows:

- events
- reservations
- reservation statuses
- outbox events
- recent event flow
- dashboard summary

This helps demonstrate backend architecture during interviews.

## Strong Interview Answer

TicketFlow is a backend-focused full-stack project where I model a realistic ticket reservation and payment orchestration flow. I designed it around consistency and reliability: PostgreSQL is the source of truth, reservation creation and outbox persistence happen in the same transaction, Kafka is used for asynchronous payment simulation, and consumers are idempotent through a processed_events table. The project is intentionally different from fraud detection because the main problem is not classification or anomaly detection; it is reservation consistency, lifecycle management, and reliable event-driven communication.

## Possible Interview Questions

### Why did you use Kafka?

To decouple reservation creation from payment processing and demonstrate asynchronous event-driven communication.

### Why did you use transactional outbox?

To avoid losing domain events after a successful database transaction and to make Kafka publishing retryable.

### Why is Idempotency-Key needed?

To prevent duplicate reservations when the client retries the same request.

### Why do consumers need to be idempotent?

Because Kafka can deliver the same message more than once. Duplicate events must not corrupt data.

### Why PostgreSQL?

Because reservation and inventory consistency require strong transactional guarantees and database constraints.

### Why is Redis included?

Redis is available for cache or helper use cases, but the early MVP can rely on PostgreSQL-backed idempotency for correctness.

### Why is frontend included?

The frontend visualizes backend behavior and makes the architecture easier to demonstrate, but the project remains backend-focused.

## Phase 0 Summary

Phase 0 is the planning and architecture phase.

No production code is written in this phase.

The purpose is to define:

- project scope
- architecture
- service responsibilities
- database design
- API design
- Kafka event flow
- Docker Compose plan
- testing strategy
- GitHub workflow
- interview explanation
