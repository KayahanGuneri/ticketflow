package com.ticketflow.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateEventRequest(

        @NotBlank(message = "Event name is required")
        @Size(max = 150, message = "Event name must be at most 150 characters")
        @Schema(example = "Spring Boot Architecture Workshop")
        String name,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        @Schema(example = "A backend-focused workshop about Spring Boot, PostgreSQL, and event-driven architecture.")
        String description,

        @NotBlank(message = "Event location is required")
        @Size(max = 255, message = "Location must be at most 255 characters")
        @Schema(example = "Istanbul")
        String location,

        @NotNull(message = "Event start time is required")
        @Future(message = "Event start time must be in the future")
        @Schema(example = "2026-07-15T19:00:00+03:00")
        OffsetDateTime startsAt,

        @NotNull(message = "Total capacity is required")
        @Positive(message = "Total capacity must be greater than zero")
        @Schema(example = "100")
        Integer totalCapacity
) {
}