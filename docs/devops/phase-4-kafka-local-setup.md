# Phase 4 DevOps — Kafka Local Setup

## Goal

This document explains the local Kafka and Kafka UI setup for Phase 4.

The goal is to run Kafka locally with Docker Compose and verify that `ticketflow-api-service` can publish outbox events.

## Docker Compose Services

Phase 4 adds:

```text
kafka
kafka-ui
```

Existing service:

```text
postgres
```

## Recommended Ports

| Service | Container Port | Host Port | Purpose |
|---|---:|---:|---|
| `postgres` | `5432` | `5435` | PostgreSQL database |
| `kafka` | `9092` | `9092` | Local Spring Boot Kafka access |
| `kafka` | `29092` | internal only | Docker network access |
| `kafka-ui` | `8080` | `8085` | Kafka UI dashboard |

## Kafka Listener Strategy

Local Spring Boot application connects through:

```text
localhost:9092
```

Kafka UI connects through Docker network:

```text
kafka:29092
```

This split prevents advertised listener problems.

## Important Environment Variables

Kafka:

```yaml
KAFKA_CFG_LISTENERS: PLAINTEXT://:29092,CONTROLLER://:9093,EXTERNAL://:9092
KAFKA_CFG_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,EXTERNAL://localhost:9092
KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,EXTERNAL:PLAINTEXT
KAFKA_CFG_INTER_BROKER_LISTENER_NAME: PLAINTEXT
ALLOW_PLAINTEXT_LISTENER: "yes"
```

Kafka UI:

```yaml
KAFKA_CLUSTERS_0_NAME: ticketflow-local
KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
```

Application:

```yaml
spring:
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

## Start Infrastructure

```powershell
docker compose -f infra/docker-compose.yml up -d
```

## Check Containers

```powershell
docker compose -f infra/docker-compose.yml ps
```

Expected containers:

```text
ticketflow-postgres
ticketflow-kafka
ticketflow-kafka-ui
```

## Check Kafka Logs

```powershell
docker logs ticketflow-kafka --tail 80
```

Expected:

```text
Kafka Server started
```

## Check Kafka UI Logs

```powershell
docker logs ticketflow-kafka-ui --tail 80
```

Expected:

```text
Started KafkaUiApplication
```

## Kafka UI

Open:

```text
http://localhost:8085
```

Expected:

```text
ticketflow-local online
```

## Verify Topic

```powershell
docker exec -it ticketflow-kafka kafka-topics.sh --bootstrap-server localhost:29092 --list
```

Expected:

```text
ticket.reservation.created
```

## Verify Messages

```powershell
docker exec -it ticketflow-kafka kafka-console-consumer.sh --bootstrap-server localhost:29092 --topic ticket.reservation.created --from-beginning --max-messages 5
```

Expected message:

```json
{
  "eventType": "ReservationCreatedEvent",
  "reservationId": "...",
  "ticketCount": 2
}
```

## Common Troubleshooting

### Kafka UI is online but no topics are visible

Start the Spring Boot application. The topic is created by `KafkaTopicConfig`.

### Spring Boot cannot connect to Kafka

Check:

```yaml
spring.kafka.bootstrap-servers=localhost:9092
```

Also verify Kafka exposes:

```text
0.0.0.0:9092->9092
```

### Kafka UI cannot connect to Kafka

Check Kafka UI bootstrap server:

```text
kafka:29092
```

Kafka UI runs inside Docker, so it should not use `localhost:9092`.

### Topic exists but no messages appear

Check:

- reservation was created
- outbox event exists
- scheduler is running
- Spring Boot log contains `Published outbox event to Kafka`
- outbox status changed to `PUBLISHED`

## Phase 4 DevOps Checklist

- [x] Kafka container runs locally
- [x] Kafka UI container runs locally
- [x] PostgreSQL remains healthy
- [x] Kafka UI shows `ticketflow-local`
- [x] topic `ticket.reservation.created` exists
- [x] Spring Boot connects to Kafka through `localhost:9092`
- [x] Kafka UI connects through `kafka:29092`
- [x] console consumer can read published messages
