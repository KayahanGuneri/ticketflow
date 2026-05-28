# Frontend Phase 1 Plan — Event Management

## Purpose

This document defines the frontend planning scope for Phase 1 of TicketFlow.

Phase 1 does not implement the full React application yet. The goal is to define how the future frontend will consume the Event Management API and how the UI should represent events and ticket inventory data.

## Frontend Role in TicketFlow

The frontend is not the main complexity of this project. It exists to make backend behavior visible and understandable.

The frontend should help demonstrate:

- event creation
- event listing
- ticket inventory visibility
- event status visibility
- future reservation flow
- future outbox and Kafka event flow visibility

The frontend must remain simple, typed, and maintainable.

## Backend APIs Needed by Frontend

### Create Event

Endpoint:

POST /api/v1/events

Frontend usage:

The frontend will use this endpoint when the user creates a new event from a future event creation form.

Required UI behavior:

- show form validation errors before sending invalid requests
- show loading state while the request is being processed
- show success state after event creation
- show API error response when backend validation fails

### List Events

Endpoint:

GET /api/v1/events

Frontend usage:

The frontend will use this endpoint to render the future EventsPage.

Required UI behavior:

- show loading state
- show empty state when there are no events
- show error state when request fails
- render event status with a badge
- show available and reserved ticket capacity if inventory fields are returned

### Get Event by ID

Endpoint:

GET /api/v1/events/{id}

Frontend usage:

The frontend will use this endpoint to render the future EventDetailPage.

Required UI behavior:

- show event metadata
- show ticket inventory summary
- show event status
- show future reservation actions when reservation phase is implemented

## Future Frontend Pages

### EventsPage

Purpose:

Display all events in the system.

Responsibilities:

- fetch event list
- render loading, error, and empty states
- render EventCard or EventTable
- navigate to EventDetailPage

### EventDetailPage

Purpose:

Display one selected event with its ticket inventory details.

Responsibilities:

- fetch event by id
- render event detail information
- render capacity summary
- prepare space for future reservation creation

### CreateEventPage

Purpose:

Allow creating a new event.

Responsibilities:

- render CreateEventForm
- validate user input
- call create event API
- show backend validation errors

This page can wait until the main frontend implementation phase if Phase 1 only requires planning.

### DashboardPage

Purpose:

Show backend system overview.

Status:

Wait until Phase 9.

### ReservationsPage

Purpose:

Show reservations and their statuses.

Status:

Wait until reservation and payment phases.

### OutboxEventsPage

Purpose:

Show transactional outbox visibility.

Status:

Wait until outbox phase.

## Future UI Components

### EventCard

Purpose:

Displays a compact event summary.

Expected props:

- event id
- event name
- location
- starts at
- status
- available capacity
- reserved capacity

### EventTable

Purpose:

Displays events in a table format.

Expected behavior:

- supports empty state
- supports status badge
- supports row click navigation

### EventStatusBadge

Purpose:

Displays event status visually.

Supported statuses:

- DRAFT
- ACTIVE
- CANCELLED

### TicketInventorySummary

Purpose:

Displays ticket capacity information.

Expected fields:

- total capacity
- available capacity
- reserved capacity

### CreateEventForm

Purpose:

Allows users to create an event.

Status:

Can wait until full frontend implementation.

### ApiErrorMessage

Purpose:

Displays backend error messages consistently.

## Clean Component Responsibility Rules

Page components should:

- fetch data
- coordinate route params
- decide loading, error, and empty states
- pass data into UI components

UI components should:

- receive props
- render data
- avoid direct API calls
- avoid business logic

API client modules should:

- call backend endpoints
- return typed responses
- centralize base URL handling
- normalize common request behavior

Type files should:

- define API request types
- define API response types
- define status union types
- avoid UI-specific concerns

## What Should Wait Until Phase 9

The following items should not be implemented in Phase 1:

- full React app setup
- routing implementation
- Tailwind layout design
- dashboard charts
- reservation UI
- payment status UI
- outbox event UI
- failed event UI
- authentication UI
- advanced filtering
- complex animations

## Phase 1 Frontend Checklist

- [ ] Define frontend role for event management
- [ ] Define future pages
- [ ] Define future UI components
- [ ] Define TypeScript types
- [ ] Define API client draft
- [ ] Define loading/error/empty state behavior
- [ ] Decide what waits until Phase 9
- [ ] Keep frontend backend-focused and simple
