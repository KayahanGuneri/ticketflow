# Docker Compose Plan

## Purpose

This document defines the local development infrastructure plan for TicketFlow.

The goal is to design the Docker Compose setup before writing the final docker-compose.yml file.

## Planned Containers

Initial local infrastructure containers:

- PostgreSQL
- Redis
- Kafka
- Kafka UI

Future application containers:

- ticketflow-api-service
- payment-simulator-service
- ticketflow-dashboard

## PostgreSQL

PostgreSQL is the main source of truth.

It stores:

- events
- ticket_inventories
- reservations
- payments
- outbox_events
- processed_events

Recommended local port:

- 5435:5432

Reason:

Using 5435 on the host avoids conflicts with any local PostgreSQL installation.

## Redis

Redis is included as an optional helper store.

Possible use cases:

- temporary idempotency helper
- lightweight caching
- future dashboard optimization

Recommended local port:

- 6379:6379

Important note:

Redis should be used only when it adds real value. PostgreSQL-backed idempotency is acceptable for the early MVP.

## Kafka

Kafka is used for asynchronous event-driven communication.

Initial topics:

- ticket.reservation.created
- ticket.payment.completed
- ticket.payment.failed
- ticket.reservation.confirmed
- ticket.reservation.cancelled
- ticket.dlq

Recommended local port:

- 9092:9092

Kafka must be accessible from:

- host machine
- ticketflow-api-service container
- payment-simulator-service container

## Kafka UI

Kafka UI is used for local development and debugging.

It helps inspect:

- topics
- messages
- consumer groups
- event flow

Recommended local port:

- 8088:8080

## Future Backend Service Ports

ticketflow-api-service:

- 8081:8081

payment-simulator-service:

- 8082:8082

## Future Frontend Port

ticketflow-dashboard:

- 5173:5173

## Recommended Local Ports

| Component | Host Port | Container Port |
| --- | --- | --- |
| PostgreSQL | 5435 | 5432 |
| Redis | 6379 | 6379 |
| Kafka | 9092 | 9092 |
| Kafka UI | 8088 | 8080 |
| ticketflow-api-service | 8081 | 8081 |
| payment-simulator-service | 8082 | 8082 |
| ticketflow-dashboard | 5173 | 5173 |

## Docker Network

All services should use the same Docker network.

Recommended network name:

- ticketflow-network

Services should communicate using Docker service names.

Examples:

- PostgreSQL hostname: postgres
- Redis hostname: redis
- Kafka hostname: kafka

## Environment Variable Naming Convention

Use clear and service-specific environment variables.

Examples for ticketflow-api-service:

- SPRING_DATASOURCE_URL
- SPRING_DATASOURCE_USERNAME
- SPRING_DATASOURCE_PASSWORD
- SPRING_KAFKA_BOOTSTRAP_SERVERS
- SPRING_DATA_REDIS_HOST
- SPRING_DATA_REDIS_PORT

Examples for payment-simulator-service:

- SPRING_KAFKA_BOOTSTRAP_SERVERS
- PAYMENT_SIMULATOR_SUCCESS_RATE
- PAYMENT_SIMULATOR_DELAY_MS

## Health Check Plan

PostgreSQL health check:

- use pg_isready

Redis health check:

- use redis-cli ping

Kafka health check:

- check broker availability

Backend services:

- use Spring Boot Actuator health endpoint later

Example future endpoints:

- /actuator/health

## Startup Order

Recommended startup order:

1. PostgreSQL
2. Redis
3. Kafka
4. Kafka UI
5. ticketflow-api-service
6. payment-simulator-service
7. ticketflow-dashboard

Important:

depends_on controls startup order but does not always guarantee readiness.

Health checks should be used where useful.

## Common DevOps Mistakes

- using localhost between containers
- forgetting Docker service names
- exposing conflicting local ports
- missing health checks
- storing secrets directly in docker-compose.yml
- not documenting ports
- starting backend before PostgreSQL is ready
- assuming Kafka is ready immediately after container startup
- making Docker setup too complex before MVP

## Phase 0 DevOps Checklist

- PostgreSQL planned
- Redis planned
- Kafka planned
- Kafka UI planned
- local ports documented
- Docker network planned
- environment variable naming convention documented
- health check strategy documented
- common mistakes documented

## Interview Explanation

In TicketFlow, Docker Compose is used to run local infrastructure consistently. PostgreSQL is the source of truth, Kafka enables asynchronous event-driven communication, Redis is available for cache or helper use cases, and Kafka UI helps inspect topics and messages during development. I planned the infrastructure before implementation to avoid port conflicts, networking mistakes, and unclear service dependencies.
