# Frontend Event Management Contract

## Purpose

This document defines the frontend-facing contract for the Event Management API.

The goal is to make the future React + TypeScript implementation easier, safer, and consistent with backend API design.

Phase 1 does not implement the full frontend application. This document only defines the contract that the future frontend will follow.

## API Endpoints

### Create Event

Method:

POST

Path:

/api/v1/events

Expected request type:

CreateEventRequest

Expected response type:

EventResponse

Success status:

201 Created

### List Events

Method:

GET

Path:

/api/v1/events

Expected response type:

EventResponse[]

Success status:

200 OK

### Get Event by ID

Method:

GET

Path:

/api/v1/events/{id}

Expected response type:

EventResponse

Success status:

200 OK

Possible error status:

404 Not Found

## TypeScript Type Drafts

```typescript
export type EventStatus = "DRAFT" | "ACTIVE" | "CANCELLED";

export interface EventResponse {
  id: string;
  name: string;
  description: string | null;
  location: string;
  startsAt: string;
  status: EventStatus;
  totalCapacity: number;
  availableCapacity: number;
  reservedCapacity: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateEventRequest {
  name: string;
  description?: string | null;
  location: string;
  startsAt: string;
  totalCapacity: number;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors: Record<string, string>;
}
```

## Backend DTO Alignment

### CreateEventRequest

Backend source:

services/ticketflow-api-service/src/main/java/com/ticketflow/event/dto/CreateEventRequest.java

Backend fields:

- name
- description
- location
- startsAt
- totalCapacity

Frontend alignment:

- name is required
- description is optional because backend does not require @NotBlank
- location is required
- startsAt is required and should be sent as an ISO date-time string
- totalCapacity is required and must be greater than zero

### EventResponse

Backend source:

services/ticketflow-api-service/src/main/java/com/ticketflow/event/dto/EventResponse.java

Backend fields:

- id
- name
- description
- location
- startsAt
- status
- totalCapacity
- availableCapacity
- reservedCapacity
- createdAt
- updatedAt

Frontend alignment:

- capacity fields are required because backend always returns them directly
- ticket inventory is not represented as a nested object in the current backend response
- date-time fields are represented as strings in frontend TypeScript
- description is nullable because backend allows it to be omitted during creation

### ApiErrorResponse

Backend source:

services/ticketflow-api-service/src/main/java/com/ticketflow/common/response/ApiErrorResponse.java

Backend fields:

- timestamp
- status
- error
- message
- path
- validationErrors

Frontend alignment:

- validationErrors must be represented as Record<string, string>
- traceId is not part of the current backend response
- traceId can be added later if backend observability is improved

## API Client Draft

```typescript
import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8081";

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

export async function createEvent(request: CreateEventRequest): Promise<EventResponse> {
  const response = await apiClient.post<EventResponse>("/api/v1/events", request);
  return response.data;
}

export async function getEvents(): Promise<EventResponse[]> {
  const response = await apiClient.get<EventResponse[]>("/api/v1/events");
  return response.data;
}

export async function getEventById(eventId: string): Promise<EventResponse> {
  const response = await apiClient.get<EventResponse>(`/api/v1/events/${eventId}`);
  return response.data;
}
```

## Frontend State Requirements

Every API-driven page should handle these states:

- loading
- success
- empty
- error

## Error Handling Plan

The frontend should display backend validation and business errors using ApiErrorResponse.

Recommended behavior:

- 400 validation error should show validationErrors near the related form fields when possible
- 400 business rule error should show message as a page-level or form-level error
- 404 event not found should show a clear not-found state
- 500 unexpected error should show a generic retry message

## Event Display Rules

Events should display:

- name
- description
- location
- startsAt
- status
- totalCapacity
- availableCapacity
- reservedCapacity

## Ticket Inventory Display Rules

Ticket inventory should be shown as a summary using the capacity fields returned by EventResponse.

Recommended UI labels:

- Total Capacity
- Available Capacity
- Reserved Capacity

The frontend should not assume a nested ticketInventory object in Phase 1 because the current backend response returns capacity fields directly.

## Date-Time Handling Rules

Backend uses OffsetDateTime.

Frontend should treat date-time values as ISO strings.

Frontend fields:

- startsAt: string
- createdAt: string
- updatedAt: string

Future UI can format these values for display, but the API contract should keep them as strings.

## Contract Verification Checklist

- [x] Backend EventResponse matches frontend EventResponse draft
- [x] Backend CreateEventRequest matches frontend CreateEventRequest draft
- [x] Backend ApiErrorResponse matches frontend ApiErrorResponse draft
- [x] Date fields are represented as ISO strings on the frontend
- [x] EventStatus values are stable
- [x] Capacity fields are returned consistently
- [x] Nested ticketInventory is not expected by frontend contract
- [x] Swagger/OpenAPI documentation visually checked against this contract

## Phase 1 Frontend Decision

The full React + TypeScript implementation should wait until Phase 9.

Phase 1 only defines:

- future frontend page needs
- API contract
- TypeScript type drafts
- error response expectations
- event and capacity display rules
- backend/frontend contract alignment
