# Phase 2 DevOps: Redis Decision and Infrastructure Notes

## Purpose

This document explains the DevOps decision for Phase 2 Backend: Reservation Transaction, Idempotency, and Optimistic Locking.

The main question for this phase is whether Redis should be introduced for idempotency or caching.

## Redis Decision

Redis is not required in Phase 2.

Phase 2 already uses PostgreSQL-backed idempotency with a unique constraint on `reservations.idempotency_key`.

This is enough for the MVP because reservation creation is a critical consistency operation. The reservation, idempotency key, and stock decrease must be protected by the same transactional boundary.

## Why Database-Backed Idempotency Is Enough

Database-backed idempotency is enough when:

- The operation creates durable business data.
- The idempotency result must survive application restarts.
- The operation must be consistent with database writes.
- The idempotency key is tied to a reservation record.
- Duplicate requests must not decrease stock twice.
- A unique constraint can protect the system at database level.

In Phase 2, `Idempotency-Key` is stored directly in the `reservations` table.

The database rule is:

```text
idempotency_key must be unique
```

This means duplicate requests cannot create duplicate reservations with the same key.

## Current Phase 2 Approach

Current reservation flow:

1. Client sends `POST /api/v1/reservations`
2. Client includes `Idempotency-Key`
3. Application checks whether the key already exists
4. If key exists with the same request, existing reservation is returned
5. If key exists with different request data, `409 Conflict` is returned
6. If key does not exist, stock is decreased and reservation is created
7. Database unique constraint protects against duplicate keys

## Why Redis Is Not Added Now

Redis is not added now because it would increase complexity without solving a real Phase 2 problem.

Adding Redis too early would require decisions about:

- Key naming
- TTL
- Serialization
- Cache invalidation
- Failure behavior
- Redis/database consistency
- Retry behavior
- Expiration policy

For this phase, PostgreSQL is safer and simpler.

## When Redis Would Be Useful Later

Redis can become useful later for:

- Short-lived request deduplication
- Rate limiting
- Temporary reservation holds
- Reservation expiration countdowns
- Fast dashboard counters
- Read-heavy cache for event availability
- Distributed locks if absolutely necessary
- Reducing repeated database reads for non-critical data

Good future Redis use cases:

```text
reservation:hold:{eventId}:{userId}
idempotency:{idempotencyKey}
event:availability:{eventId}
rate-limit:user:{userId}
```

## Redis for Idempotency: Future Design Option

A future Redis-backed idempotency flow could look like:

1. Client sends `Idempotency-Key`
2. API checks Redis first
3. If Redis has completed response, return cached response
4. If Redis has processing marker, reject or wait
5. If Redis has no key, continue to database transaction
6. Store final result in Redis with TTL

Possible Redis key:

```text
idempotency:reservation:{idempotencyKey}
```

Possible TTL:

```text
24 hours
```

Important: Redis idempotency should not replace database constraints for critical reservation consistency.

## Optional Docker Compose Redis Section

Redis is not required in Phase 2, but if we decide to add it later, this Docker Compose section can be used:

```yaml
redis:
  image: redis:7.2-alpine
  container_name: ticketflow-redis
  ports:
    - "6379:6379"
  command: ["redis-server", "--appendonly", "yes"]
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 10s
    timeout: 5s
    retries: 5
  volumes:
    - redis-data:/data
```

Volume section:

```yaml
volumes:
  redis-data:
```

## Optional Spring Redis Configuration Plan

If Redis is introduced later, add dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

Example properties:

```yaml
spring:
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: ${SPRING_DATA_REDIS_PORT:6379}
      timeout: 2s
```

Useful environment variables:

```text
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
```

## Redis Verification Commands

If Redis is added later, verify it with:

```powershell
docker ps
```

```powershell
docker exec -it ticketflow-redis redis-cli ping
```

Expected output:

```text
PONG
```

Manual key test:

```powershell
docker exec -it ticketflow-redis redis-cli set ticketflow:test "ok"
docker exec -it ticketflow-redis redis-cli get ticketflow:test
docker exec -it ticketflow-redis redis-cli del ticketflow:test
```

Expected output:

```text
ok
```

## Common Redis Mistakes

Avoid these mistakes:

- Using Redis as the only protection for critical stock consistency
- Forgetting TTL for temporary idempotency keys
- Caching reservation state without invalidation strategy
- Using Redis locks before understanding database transactions
- Ignoring Redis failure behavior
- Storing large response payloads unnecessarily
- Using Redis for every problem
- Adding Redis before a real bottleneck exists
- Treating Redis data as permanently durable
- Forgetting environment-based configuration

## Phase 2 DevOps Checklist

- Redis decision documented
- Database-backed idempotency accepted for Phase 2
- Redis not added to runtime infrastructure yet
- Optional Docker Compose Redis section documented
- Optional Spring Redis configuration documented
- Redis verification commands documented
- Common Redis mistakes documented
- No unnecessary infrastructure added before MVP need
