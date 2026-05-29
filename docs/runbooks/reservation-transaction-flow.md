# Reservation Transaction Flow

## Purpose

This document explains the Phase 2 backend reservation flow for TicketFlow.

The goal of this phase is to create reservations safely while protecting ticket stock consistency under duplicate requests and concurrent reservation attempts.

## Implemented Features

- Reservation entity
- ReservationStatus enum
- Reservation creation API
- Reservation listing API
- Reservation detail API
- Database-backed Idempotency-Key handling
- Transactional stock decrease
- Optimistic locking with ticket inventory version field
- Conflict handling
- Unit and controller tests

## API Endpoints

### Create Reservation

```http
POST /api/v1/reservations
Idempotency-Key: reservation-key-001
Content-Type: application/json

Request body:

{
  "eventId": "uuid",
  "userId": "uuid",
  "ticketCount": 2
}

Successful new reservation response:

201 Created

Idempotent retry response:

200 OK
List Reservations
GET /api/v1/reservations
Get Reservation By Id
GET /api/v1/reservations/{id}
Transaction Boundary

Reservation creation is handled inside the service layer with @Transactional.

The following operations happen in the same transaction:

Normalize and validate Idempotency-Key
Check whether the idempotency key already exists
Load event
Load ticket inventory
Decrease available capacity
Increase reserved capacity
Persist reservation

If any step fails, the transaction rolls back.

Idempotency Behavior

The Idempotency-Key header is required for reservation creation.

If the same key is reused with the same request payload:

The existing reservation is returned
Stock is not decreased again
Response status is 200 OK

If the same key is reused with a different request payload:

The request is rejected
Response status is 409 Conflict

This prevents duplicate reservation creation caused by client retries, network failures, or repeated form submissions.

Stock Consistency

Ticket stock is managed by TicketInventory.

When a reservation is created:

availableCapacity = availableCapacity - ticketCount
reservedCapacity = reservedCapacity + ticketCount

The database also protects consistency with check constraints:

available_capacity >= 0
reserved_capacity >= 0
available_capacity + reserved_capacity = total_capacity
Optimistic Locking

TicketInventory has a version field annotated with @Version.

This helps prevent lost updates when multiple reservation requests try to update the same ticket inventory concurrently.

If concurrent updates conflict, the API returns:

409 Conflict
Error Handling

Common error responses:

Scenario    HTTP Status
Missing Idempotency-Key    400 Bad Request
Validation error    400 Bad Request
Event not found    404 Not Found
Insufficient stock    409 Conflict
Same Idempotency-Key with different request    409 Conflict
Optimistic locking conflict    409 Conflict
Manual Validation Results

Validated scenarios:

Created event with capacity 5
Created reservation for 2 tickets
Verified stock changed from 5/0 to 3/2
Sent duplicate request with same Idempotency-Key
Verified same reservation was returned
Verified stock did not decrease again
Reused same Idempotency-Key with different request
Verified 409 Conflict
Requested more tickets than available
Verified 409 Conflict
Sent request without Idempotency-Key
Verified 400 Bad Request
Verified reservation list endpoint
Verified reservation detail endpoint
Verified first reservation request returns 201 Created
Verified idempotent retry returns 200 OK
Test Coverage

Implemented tests:

TicketInventoryTest
ReservationServiceTest
ReservationControllerTest

Test result:

Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Production Notes

In a production system:

Idempotency keys should usually include a TTL or expiration policy.
A dedicated idempotency table can store request hash, response payload, status, and expiration time.
Redis can be introduced later for short-lived idempotency or request deduplication, but PostgreSQL-backed idempotency is safer for the MVP.
Kafka publishing should not happen directly inside the reservation transaction.
The next phase should introduce the Transactional Outbox Pattern.
