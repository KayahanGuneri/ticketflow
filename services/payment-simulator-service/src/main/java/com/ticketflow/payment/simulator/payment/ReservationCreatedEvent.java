package com.ticketflow.payment.simulator.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * @author Kayahan Güneri
 * Purpose: Represents the reservation-created event consumed from Kafka.
 * Date: 2026-06-01
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReservationCreatedEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        OffsetDateTime occurredAt,
        UUID reservationId,
        UUID ticketEventId,
        UUID userId,
        Integer ticketCount
) {

    public static final String EVENT_TYPE = "ReservationCreatedEvent";

    public boolean isProcessable() {
        return eventId != null
                && EVENT_TYPE.equals(eventType)
                && aggregateId != null
                && occurredAt != null
                && reservationId != null
                && userId != null
                && ticketCount != null
                && ticketCount > 0;
    }
}