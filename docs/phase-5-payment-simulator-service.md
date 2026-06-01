# Phase 5 — Payment Simulator Service

## Purpose

This phase adds `payment-simulator-service` to TicketFlow.

The service simulates an external payment provider in the reservation flow.

Its main responsibility is to consume `ReservationCreatedEvent` from Kafka, simulate payment success or failure, and publish a payment result event.

## Why This Service Exists

`payment-simulator-service` is a separate service because payment processing is an external asynchronous dependency in real systems.

In a production-like ticket reservation system, reservation creation and payment processing should not be tightly coupled in the same synchronous request.

This service demonstrates:

- service boundaries
- asynchronous communication
- Kafka consumer behavior
- Kafka producer behavior
- external provider simulation
- eventual consistency
- small and focused service design

## Service Responsibilities

`payment-simulator-service` is responsible for:

- consuming `ReservationCreatedEvent`
- validating the consumed event shape
- simulating payment success or failure
- publishing `PaymentCompletedEvent`
- publishing `PaymentFailedEvent`
- logging consume and publish actions
- exposing Spring Boot Actuator health endpoint

It does not own reservation state.

It does not own ticket inventory.

It does not use a database in the MVP.

## Kafka Topics

Consumed topic:

- `ticket.reservation.created`

Produced topics:

- `ticket.payment.completed`
- `ticket.payment.failed`

## Event Flow

    ticketflow-api-service
            |
            | publishes ReservationCreatedEvent
            v
    ticket.reservation.created
            |
            | consumes
            v
    payment-simulator-service
            |
            | simulates payment result
            |
            | success -> PaymentCompletedEvent
            | failure -> PaymentFailedEvent
            v
    Kafka

## Success Flow

1. `ReservationCreatedEvent` is consumed.
2. Payment simulation returns `COMPLETED`.
3. `PaymentCompletedEvent` is published to `ticket.payment.completed`.

## Failure Flow

1. `ReservationCreatedEvent` is consumed.
2. Payment simulation returns `FAILED`.
3. `PaymentFailedEvent` is published to `ticket.payment.failed`.

## Configuration

Local Maven mode:

    spring:
      kafka:
        bootstrap-servers: localhost:9092

Docker Compose mode:

    SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092

Payment simulation variables:

    PAYMENT_SIMULATOR_SUCCESS_RATE=80
    PAYMENT_SIMULATOR_DELAY_MS=500
    PAYMENT_SIMULATOR_DEFAULT_AMOUNT=100.00

## Docker Compose

The service is added to Docker Compose as:

    payment-simulator-service:
      build:
        context: ../services/payment-simulator-service
        dockerfile: Dockerfile
      container_name: ticketflow-payment-simulator-service
      depends_on:
        kafka:
          condition: service_healthy
      ports:
        - "8082:8082"
      environment:
        SPRING_PROFILES_ACTIVE: local
        SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
        PAYMENT_SIMULATOR_SUCCESS_RATE: 80
        PAYMENT_SIMULATOR_DELAY_MS: 500
        PAYMENT_SIMULATOR_DEFAULT_AMOUNT: 100.00

## Manual Verification

### Build the service

    cd C:\Users\kayah\Desktop\ticketflow\services\payment-simulator-service
    mvn clean package

Expected result:

    BUILD SUCCESS

### Build Docker image

    cd C:\Users\kayah\Desktop\ticketflow
    docker compose -f infra/docker-compose.yml build payment-simulator-service

Expected result:

    payment-simulator-service Built

### Start Docker Compose

    docker compose -f infra/docker-compose.yml up -d

Expected containers:

- `ticketflow-postgres`
- `ticketflow-kafka`
- `ticketflow-kafka-ui`
- `ticketflow-payment-simulator-service`

### Check running containers

    docker compose -f infra/docker-compose.yml ps

Expected result:

    ticketflow-payment-simulator-service   Up
    ticketflow-kafka                       Up / healthy
    ticketflow-kafka-ui                    Up
    ticketflow-postgres                    Up / healthy

### Check logs

    docker compose -f infra/docker-compose.yml logs -f payment-simulator-service

Expected log examples:

    Started PaymentSimulatorServiceApplication
    Subscribed to topic(s): ticket.reservation.created
    bootstrap.servers = [kafka:29092]

### Health check

    Invoke-RestMethod http://localhost:8082/actuator/health

Expected result:

    status
    ------
    UP

### Produce a test message

Kafka UI:

    http://localhost:8085

Topic:

    ticket.reservation.created

Example key:

    77777777-7777-7777-7777-777777777777

Example payload:

    {
      "eventId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
      "eventType": "ReservationCreatedEvent",
      "aggregateId": "77777777-7777-7777-7777-777777777777",
      "occurredAt": "2026-06-01T11:45:00+03:00",
      "reservationId": "77777777-7777-7777-7777-777777777777",
      "ticketEventId": "88888888-8888-8888-8888-888888888888",
      "userId": "99999999-9999-9999-9999-999999999999",
      "ticketCount": 2
    }

Expected result:

- `ticket.payment.completed` receives `PaymentCompletedEvent`, or
- `ticket.payment.failed` receives `PaymentFailedEvent`.

## Testing

Implemented test classes:

- `PaymentSimulatorTest`
- `PaymentResultProducerTest`
- `ReservationCreatedConsumerTest`

Verified behavior:

- successful payment simulation
- failed payment simulation
- invalid event skipping
- completed event publishing
- failed event publishing
- Kafka key uses `reservationId`

## OOP and SOLID Notes

The service follows a small and focused design:

- `ReservationCreatedConsumer` handles Kafka consuming.
- `PaymentSimulator` handles payment simulation logic.
- `PaymentResultProducer` handles Kafka publishing.
- Event records represent stable Kafka payload contracts.
- Configuration properties are separated from business logic.
- Kafka topic names are managed through configuration properties.

The service avoids overengineering because the MVP does not require a database, real payment provider, or complex retry/DLQ handling.

## Current Limitations

- The service is stateless.
- It does not persist payment attempts.
- Durable idempotency is not implemented in this service.
- Retry and DLQ behavior will be improved in a later phase.
- The main API service will later consume payment result events and update reservation state.

## Common Junior Mistakes Avoided

- Putting payment simulation logic inside the Kafka consumer.
- Hardcoding Kafka topic names in multiple classes.
- Adding a database before there is a real MVP need.
- Publishing JPA entities as Kafka messages.
- Using `localhost:9092` inside Docker containers.
- Ignoring Kafka's at-least-once delivery model.
- Logging entire payloads unnecessarily.

## Interview Explanation

`payment-simulator-service` simulates an external payment provider.

It consumes `ReservationCreatedEvent` from Kafka, applies a configurable success-rate based simulation, and publishes either `PaymentCompletedEvent` or `PaymentFailedEvent`.

I kept it stateless and small for the MVP because its responsibility is only to demonstrate asynchronous service communication.

The main API service owns reservation state transitions and durable idempotency.

## 30-Second Explanation

In Phase 5, I added `payment-simulator-service`, a separate Spring Boot service that simulates an external payment provider.

It consumes `ReservationCreatedEvent` from Kafka, runs a configurable success/failure simulation, and publishes either `PaymentCompletedEvent` or `PaymentFailedEvent`.

This demonstrates asynchronous service communication and keeps payment logic separate from reservation creation.

## 2-Minute Explanation

In Phase 5, I introduced `payment-simulator-service` as a separate backend service to model an external payment provider.

In real systems, payment processing is usually asynchronous and should not block the reservation creation request.

The main API service publishes `ReservationCreatedEvent` to Kafka through the transactional outbox flow.

The payment simulator consumes this event from `ticket.reservation.created`, validates the event shape, simulates a payment result using a configurable success rate, and then publishes either `PaymentCompletedEvent` to `ticket.payment.completed` or `PaymentFailedEvent` to `ticket.payment.failed`.

The service is intentionally stateless in the MVP.

It does not own reservation state, ticket inventory, or payment persistence.

Those responsibilities remain in `ticketflow-api-service`.

This keeps the service small, focused, and aligned with SOLID principles.

I also containerized the service with Docker Compose and configured it to connect to Kafka using `kafka:29092` inside the Docker network.

This verified that the service works both locally and in the containerized development environment.
