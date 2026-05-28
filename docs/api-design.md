# API Design

## Purpose

This document defines the initial REST API design for TicketFlow.

The API must be simple, consistent, backend-focused, and easy to explain in a technical interview.

## General API Rules

- Use /api/v1 as the base path.
- Use DTOs for request and response models.
- Do not expose JPA entities directly.
- Use validation annotations for request DTOs.
- Use consistent error responses.
- Use Idempotency-Key header for reservation creation.

## Standard Error Response

All API errors should follow a consistent response structure.

Fields:

- timestamp
- status
- error
- message
- path
- traceId

Example:

{
  "timestamp": "2026-05-28T16:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "ticketCount must be greater than 0",
  "path": "/api/v1/reservations",
  "traceId": "optional-trace-id"
}

## Event APIs

### POST /api/v1/events

Purpose:

Creates a new ticketed event.

Request body:

{
  "name": "Rock Festival 2026",
  "description": "Outdoor music festival",
  "location": "Istanbul",
  "startsAt": "2026-08-15T20:00:00Z",
  "totalCapacity": 1000
}

Response body:

{
  "id": "uuid",
  "name": "Rock Festival 2026",
  "description": "Outdoor music festival",
  "location": "Istanbul",
  "startsAt": "2026-08-15T20:00:00Z",
  "status": "DRAFT",
  "totalCapacity": 1000,
  "availableCapacity": 1000,
  "reservedCapacity": 0,
  "createdAt": "timestamp"
}

Validation rules:

- name must not be blank
- startsAt must be a future date
- totalCapacity must be positive

### GET /api/v1/events

Purpose:

Lists events.

Possible query parameters:

- status
- page
- size

Response:

Returns a paginated list of events.

### GET /api/v1/events/{id}

Purpose:

Returns event details by ID.

Possible errors:

- 404 ResourceNotFoundException

## Reservation APIs

### POST /api/v1/reservations

Purpose:

Creates a ticket reservation.

Required header:

Idempotency-Key: unique-client-generated-key

Request body:

{
  "eventId": "uuid",
  "userId": "uuid",
  "ticketCount": 2
}

Response body:

{
  "id": "uuid",
  "eventId": "uuid",
  "userId": "uuid",
  "ticketCount": 2,
  "status": "PENDING",
  "createdAt": "timestamp"
}

Business rules:

- Idempotency-Key is required.
- eventId must exist.
- event must be active or reservable.
- ticketCount must be positive.
- available capacity must be enough.
- duplicate Idempotency-Key must return the existing reservation.

Possible errors:

- 400 Validation error
- 404 Event not found
- 409 InsufficientTicketCapacityException
- 409 DuplicateRequestException if request conflicts with previous idempotency usage

### GET /api/v1/reservations

Purpose:

Lists reservations.

Possible query parameters:

- eventId
- userId
- status
- page
- size

### GET /api/v1/reservations/{id}

Purpose:

Returns reservation details by ID.

Possible errors:

- 404 ResourceNotFoundException

## Dashboard APIs

### GET /api/v1/dashboard/summary

Purpose:

Returns high-level dashboard metrics.

Example response:

{
  "totalEvents": 10,
  "activeEvents": 6,
  "totalReservations": 250,
  "pendingReservations": 20,
  "confirmedReservations": 200,
  "cancelledReservations": 30,
  "pendingOutboxEvents": 3,
  "failedOutboxEvents": 1
}

### GET /api/v1/dashboard/recent-reservations

Purpose:

Returns recent reservations for dashboard visibility.

### GET /api/v1/dashboard/recent-events

Purpose:

Returns recent domain or system events for dashboard visibility.

## Outbox APIs

### GET /api/v1/outbox-events

Purpose:

Lists outbox events for debugging and visibility.

Possible query parameters:

- status
- eventType
- page
- size

## Processed Event APIs

### GET /api/v1/processed-events

Purpose:

Lists processed Kafka events for consumer idempotency visibility.

Possible query parameters:

- eventType
- consumerName
- page
- size

## API Design Decisions

### Why DTOs Are Required

DTOs protect the API contract from persistence details.

They also prevent accidental exposure of internal fields.

### Why Idempotency-Key Is a Header

Idempotency-Key belongs to request metadata.

It represents the identity of a client retry attempt, not the reservation business payload itself.

### Why Dashboard APIs Exist

Dashboard APIs make backend behavior visible.

They are useful for demo, debugging, and technical interview explanation.

## Interview Explanation

In TicketFlow, the REST API is intentionally simple and consistent. Controllers receive DTOs, validation protects input, services handle business rules, and responses never expose JPA entities directly. Reservation creation requires an Idempotency-Key header to protect the system from duplicate client retries.
