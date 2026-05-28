# Kafka Event Flow

## Purpose

This document defines the initial Kafka topic and event flow design for TicketFlow.

Kafka is used for asynchronous communication between ticketflow-api-service and payment-simulator-service.

## Kafka Design Goals

- decouple reservation creation from payment simulation
- demonstrate event-driven architecture
- support eventual consistency
- support reliable event publishing with transactional outbox
- support idempotent consumers
- document retry and DLQ concepts

## Kafka Delivery Assumption

Kafka consumers must assume at-least-once delivery.

This means the same event may be delivered more than once.

Because of this, consumers must be idempotent.

## Topics

Initial topics:

- ticket.reservation.created
- ticket.payment.completed
- ticket.payment.failed
- ticket.reservation.confirmed
- ticket.reservation.cancelled
- ticket.dlq

## Events

Initial events:

- ReservationCreatedEvent
- PaymentCompletedEvent
- PaymentFailedEvent
- ReservationConfirmedEvent
- ReservationCancelledEvent

## Common Event Fields

Every event should include:

- eventId
- eventType
- aggregateId
- occurredAt

Rules:

- eventId must be unique.
- eventType must be explicit.
- aggregateId should represent the main business entity ID.
- occurredAt must represent when the event happened.
- events should be immutable DTOs or records if possible.
- JPA entities must not be published as Kafka payloads.

## ReservationCreatedEvent

Topic:

- ticket.reservation.created

Producer:

- ticketflow-api-service outbox publisher

Consumer:

- payment-simulator-service

Example payload:

{
  "eventId": "uuid",
  "eventType": "ReservationCreatedEvent",
  "aggregateId": "reservation-id",
  "reservationId": "reservation-id",
  "ticketEventId": "event-id",
  "userId": "user-id",
  "ticketCount": 2,
  "occurredAt": "timestamp"
}

Purpose:

Notifies payment-simulator-service that a reservation was created and payment simulation can start.

## PaymentCompletedEvent

Topic:

- ticket.payment.completed

Producer:

- payment-simulator-service

Consumer:

- ticketflow-api-service

Example payload:

{
  "eventId": "uuid",
  "eventType": "PaymentCompletedEvent",
  "aggregateId": "reservation-id",
  "reservationId": "reservation-id",
  "paymentId": "payment-id",
  "amount": 100.00,
  "occurredAt": "timestamp"
}

Purpose:

Notifies ticketflow-api-service that the payment succeeded.

Expected result:

- reservation status becomes CONFIRMED
- payment status becomes COMPLETED
- event is stored in processed_events

## PaymentFailedEvent

Topic:

- ticket.payment.failed

Producer:

- payment-simulator-service

Consumer:

- ticketflow-api-service

Example payload:

{
  "eventId": "uuid",
  "eventType": "PaymentFailedEvent",
  "aggregateId": "reservation-id",
  "reservationId": "reservation-id",
  "paymentId": "payment-id",
  "failureReason": "SIMULATED_PAYMENT_FAILURE",
  "occurredAt": "timestamp"
}

Purpose:

Notifies ticketflow-api-service that the payment failed.

Expected result:

- reservation status becomes CANCELLED
- payment status becomes FAILED
- ticket stock is restored safely
- event is stored in processed_events

## ReservationConfirmedEvent

Topic:

- ticket.reservation.confirmed

Producer:

- ticketflow-api-service

Consumer:

- optional future consumers

Purpose:

Can be used later for notification or analytics.

This event may be optional in the early MVP.

## ReservationCancelledEvent

Topic:

- ticket.reservation.cancelled

Producer:

- ticketflow-api-service

Consumer:

- optional future consumers

Purpose:

Can be used later for notification, analytics, or customer communication.

This event may be optional in the early MVP.

## Main Event Flow

### Step 1: Reservation Created

1. Client calls POST /api/v1/reservations.
2. ticketflow-api-service creates reservation in PostgreSQL.
3. ticketflow-api-service updates ticket inventory in PostgreSQL.
4. ticketflow-api-service saves ReservationCreatedEvent in outbox_events.
5. Database transaction commits.

### Step 2: Outbox Publishing

1. Outbox publisher reads PENDING outbox events.
2. Outbox publisher publishes ReservationCreatedEvent to ticket.reservation.created.
3. Outbox event status becomes PUBLISHED.
4. If publish fails, retry_count is increased and error_message is saved.

### Step 3: Payment Simulation

1. payment-simulator-service consumes ReservationCreatedEvent.
2. It simulates payment success or failure.
3. It publishes PaymentCompletedEvent or PaymentFailedEvent.

### Step 4: Payment Result Processing

1. ticketflow-api-service consumes payment result event.
2. It checks processed_events.
3. If event was already processed, it ignores the event safely.
4. If payment completed, reservation becomes CONFIRMED.
5. If payment failed, reservation becomes CANCELLED and stock is restored.
6. processed_events record is saved.

## Retry and DLQ Concept

Retry and DLQ should be designed carefully.

Initial concept:

- temporary failures can be retried
- permanent failures can be moved to ticket.dlq
- DLQ messages should include original topic, original payload, error message, and failure timestamp

DLQ purpose:

- prevent poison messages from blocking consumers
- allow manual inspection
- support debugging and recovery

## Idempotency Rules

Producer side:

- outbox prevents lost events after database commit
- outbox status prevents repeated successful publishing when managed correctly

Consumer side:

- processed_events prevents duplicate event processing
- eventId and consumerName should be unique together

## Interview Explanation

In TicketFlow, Kafka is used to decouple reservation creation from payment processing. The API service writes ReservationCreatedEvent to an outbox table in the same transaction as the reservation. A publisher later sends that event to Kafka. The payment simulator consumes it and publishes a payment result event. The API service consumes the payment result idempotently using a processed_events table, so duplicate Kafka delivery does not corrupt reservation or stock state.
