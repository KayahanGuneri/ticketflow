package com.ticketflow.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * @author Kayahan Güneri
 * Purpose: Carries client input required to create a ticket reservation.
 * Date: 2026-05-29
 */
public record CreateReservationRequest(
        @NotNull(message = "Event id is required")
        UUID eventId,

        @NotNull(message = "User id is required")
        UUID userId,

        @NotNull(message = "Ticket count is required")
        @Positive(message = "Ticket count must be positive")
        Integer ticketCount
) {
}