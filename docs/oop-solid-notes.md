# OOP and SOLID Notes

## Purpose

This document defines how OOP and SOLID principles should be applied in TicketFlow.

The goal is not to overengineer the project.

The goal is to keep the code understandable, maintainable, testable, and realistic.

## OOP Principles

### Encapsulation

Business rules must be protected inside the correct classes and services.

Examples:

- reservation creation rules belong in reservation service logic
- ticket capacity changes must not be scattered across controllers
- reservation status transitions must be explicit
- entity internals should not be exposed directly through REST APIs

Bad direction:

- putting stock update logic directly inside controller methods
- returning JPA entities directly to API clients
- allowing random classes to modify reservation status without rules

Good direction:

- keep HTTP logic in controllers
- keep business decisions in services
- keep persistence details in repositories
- use DTOs for API input and output

### Abstraction

Abstraction should be used when it improves clarity.

TicketFlow should not create interfaces for every class automatically.

Use abstraction when:

- it improves testability
- it separates infrastructure from business flow
- it makes a complex concept easier to understand
- there are multiple realistic implementations

Do not use abstraction only to look enterprise.

### Inheritance

Inheritance should be avoided unless there is a strong reason.

Prefer composition over inheritance.

Most TicketFlow classes should use simple composition through constructor injection.

### Polymorphism

Polymorphism can be useful later for payment simulation strategies or event handling strategies.

For the first MVP, use it only if it keeps the code simpler.

Possible future example:

- PaymentSimulationStrategy
- SuccessPaymentSimulationStrategy
- FailedPaymentSimulationStrategy

But do not introduce this too early unless needed.

## SOLID Principles

### Single Responsibility Principle

Each class should have one clear reason to change.

Expected responsibilities:

- EventController handles event HTTP endpoints
- EventService handles event use cases
- EventRepository handles event persistence
- ReservationController handles reservation HTTP endpoints
- ReservationService handles reservation business flow
- OutboxService handles outbox persistence logic
- OutboxPublisher handles Kafka publishing from outbox
- PaymentResultConsumer handles payment result events
- GlobalExceptionHandler handles API error responses

Controller classes must not become business logic containers.

Service classes must not become God classes.

### Open Closed Principle

Code should be open for extension but closed for fragile modification.

Apply this pragmatically.

Good examples:

- keep event type names centralized
- keep status transition logic explicit
- avoid duplicated condition logic across classes

Do not create unnecessary strategy patterns before the MVP needs them.

### Liskov Substitution Principle

Avoid inheritance-heavy designs.

If inheritance is used, subclasses must be safely replaceable by the parent type.

In TicketFlow, composition is usually safer than inheritance.

### Interface Segregation Principle

Do not create large interfaces.

Do not create interfaces automatically for every service.

Create interfaces only when they provide clear value.

Good interface candidates later:

- PaymentSimulationStrategy
- EventPublisher

Bad direction:

- HugeApplicationService interface
- CommonManager interface with unrelated methods

### Dependency Inversion Principle

High-level business logic should not depend directly on low-level infrastructure details.

Use constructor injection.

Avoid field injection.

Business services should depend on repositories, publishers, or focused collaborators through clear boundaries.

## Layering Rules

### Controller Layer

Allowed:

- receive HTTP requests
- validate request DTOs
- call service methods
- return response DTOs

Not allowed:

- business decisions
- transaction orchestration
- direct EntityManager usage
- Kafka publishing
- stock update logic

### Service Layer

Allowed:

- use case orchestration
- transaction boundaries
- business validation
- state transitions
- repository coordination
- outbox event creation

Not allowed:

- raw HTTP concerns
- returning JPA entities directly to clients
- unrelated responsibilities in one huge service

### Repository Layer

Allowed:

- database access
- query methods
- persistence-focused operations

Not allowed:

- business orchestration
- API response shaping
- Kafka event publishing

### DTO Layer

DTOs define API contracts.

DTOs must be separate from JPA entities.

Use request DTOs for input and response DTOs for output.

## Domain State Rules

Important enums:

- EventStatus
- ReservationStatus
- PaymentStatus
- OutboxStatus

State transitions must be clear and controlled.

Example reservation lifecycle:

- PENDING after reservation creation
- CONFIRMED after successful payment
- CANCELLED after failed payment
- EXPIRED if expiration is implemented later

## Interview Explanation

In an interview, explain this simply:

TicketFlow uses OOP and SOLID pragmatically. Controllers only handle HTTP, services handle business use cases, repositories handle persistence, and Kafka publishers or consumers handle messaging responsibilities. I avoid putting business logic into controllers and I keep DTOs separate from JPA entities. I also avoid unnecessary abstractions early, but I keep the design extensible where it has real value.
