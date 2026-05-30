package com.ticketflow.reservation.event;

import com.ticketflow.reservation.entity.Reservation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationCreatedEvent(
        UUID messageId,
        String eventType,
        UUID reservationId,
        UUID ticketEventId,
        UUID userId,
        int ticketCount,
        OffsetDateTime occurredAt
) {
    public static final String EVENT_TYPE = "ReservationCreatedEvent";

    public static ReservationCreatedEvent from(Reservation reservation) {
        return new ReservationCreatedEvent(
                UUID.randomUUID(),
                EVENT_TYPE,
                reservation.getId(),
                reservation.getEvent().getId(),
                reservation.getUserId(),
                reservation.getTicketCount(),
                OffsetDateTime.now()
        );
    }
}