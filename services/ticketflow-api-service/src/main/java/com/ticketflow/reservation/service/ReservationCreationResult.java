package com.ticketflow.reservation.service;

import com.ticketflow.reservation.dto.ReservationResponse;

/**
 * @author Kayahan Güneri
 * Purpose: Carries reservation creation response metadata for distinguishing new creations from idempotent retries.
 * Date: 2026-05-29
 */
public record ReservationCreationResult(
        ReservationResponse response,
        boolean created
) {
}