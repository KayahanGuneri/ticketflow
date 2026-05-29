package com.ticketflow.reservation.dto;

import com.ticketflow.reservation.entity.ReservationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * @author Kayahan Güneri
 * Purpose: Represents reservation data returned by reservation APIs.
 * Date: 2026-05-29
 */
public record ReservationResponse(
        UUID id,
        UUID eventId,
        UUID userId,
        int ticketCount,
        ReservationStatus status,
        String idempotencyKey,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}