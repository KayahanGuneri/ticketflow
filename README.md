# TicketFlow

TicketFlow is an event-driven ticket reservation and order orchestration platform built as a backend-focused full-stack portfolio project.

The goal of this project is to demonstrate realistic backend engineering skills beyond simple CRUD development.

The system focuses on ticket inventory consistency, reservation orchestration, asynchronous payment simulation, transactional outbox, Kafka-based communication, idempotency, retry concepts, and production-oriented documentation.

## Project Purpose

TicketFlow simulates a real-world ticket reservation flow:

1. An event is created.
2. Ticket inventory is assigned to the event.
3. A user creates a reservation request.
4. The system reserves ticket capacity safely.
5. A reservation event is written to the outbox table.
6. The outbox publisher publishes the event to Kafka.
7. The payment simulator consumes the reservation event.
8. The payment simulator publishes a payment result event.
9. The main API service consumes the payment result.
10. The reservation is confirmed or cancelled based on the payment result.

## Why This Project Exists

This project exists to practice and demonstrate:

- Java 21
- Spring Boot 3.x
- PostgreSQL
- Apache Kafka
- Redis where useful
- Transactional Outbox Pattern
- Idempotency
- Optimistic locking
- Event-driven architecture
- Clean Code
- OOP and SOLID principles
- Docker Compose local infrastructure
- Unit and integration testing
- React + TypeScript dashboard
- Technical interview explanation skills

## Why TicketFlow Is Different From a Fraud or Anomaly Project

TicketFlow is not a fraud detection, anomaly detection, suspicious transaction monitoring, or payment fraud project.

The main focus is not detecting suspicious behavior.

The main focus is:

- ticket capacity consistency
- reservation lifecycle management
- asynchronous payment orchestration
- reliable event publishing
- idempotent event consumption
- eventual consistency between services

This makes the project closer to a real reservation/order orchestration system than a monitoring or anomaly dashboard.

## Target Architecture

The project uses a monorepo structure.

Main components:

- ticketflow-api-service
- payment-simulator-service
- ticketflow-dashboard
- PostgreSQL
- Redis
- Kafka
- Kafka UI

## Main Backend Service

ticketflow-api-service is responsible for:

- event management
- ticket inventory management
- reservation creation
- stock consistency
- idempotency handling
- transactional outbox persistence
- Kafka publishing from outbox
- payment result consumption
- reservation finalization
- dashboard APIs

## Payment Simulator Service

payment-simulator-service is responsible for:

- consuming ReservationCreatedEvent
- simulating payment success or failure
- publishing PaymentCompletedEvent or PaymentFailedEvent

## Frontend Dashboard

ticketflow-dashboard is responsible for:

- showing event data
- showing reservations
- showing reservation statuses
- showing outbox events
- showing recent event flow
- supporting backend visibility

The frontend is not the main part of the project. It exists to demonstrate and visualize backend architecture.

## MVP Scope

The MVP includes:

- Event creation
- Ticket inventory creation
- Ticket reservation API
- Idempotency-Key support
- PostgreSQL transaction handling
- Optimistic locking
- Transactional Outbox Pattern
- Kafka event publishing
- Payment simulator service
- Payment success/failure handling
- Reservation status lifecycle
- Processed event tracking
- Basic retry and DLQ concept
- Dashboard summary APIs
- React + TypeScript dashboard
- Docker Compose local infrastructure
- Unit and integration tests
- Professional documentation

## Out of Scope Before MVP

The following features are intentionally excluded before the MVP is complete:

- Kubernetes
- Real payment gateway integration
- Full authentication and authorization
- Complex RBAC
- Prometheus and Grafana
- Cloud deployment
- Multi-tenant SaaS structure
- Machine learning
- Mobile application
- Overly complex frontend animations

## Tech Stack

| Area | Technology |
| --- | --- |
| Backend Language | Java 21 |
| Backend Framework | Spring Boot 3.x |
| Build Tool | Maven |
| Database | PostgreSQL |
| Messaging | Apache Kafka |
| Cache / Helper Store | Redis |
| Frontend | React + TypeScript + Vite |
| Styling | TailwindCSS |
| Local Infrastructure | Docker Compose |
| Testing | JUnit, Mockito, Spring Boot Test, Testcontainers |

## Repository Structure

- README.md
- .gitignore
- docs/
- services/
- frontend/
- infra/
- scripts/

## Current Phase

Current branch:

feature/payment-simulator

Current phase:

Phase 5 — Payment Simulator Service

This phase adds `payment-simulator-service`, a small Spring Boot service that consumes `ReservationCreatedEvent`, simulates payment success or failure, and publishes `PaymentCompletedEvent` or `PaymentFailedEvent` through Kafka.

Phase 5 is verified with:

- Maven tests
- Docker Compose build
- Docker Compose runtime
- Kafka UI manual event production
- `ticket.payment.completed` / `ticket.payment.failed` output topic verification
