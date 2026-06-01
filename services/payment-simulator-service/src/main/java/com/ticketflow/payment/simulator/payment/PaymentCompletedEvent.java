package com.ticketflow.payment.simulator.payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * @author Kayahan Güneri
 * Purpose: Represents a successful payment result event published to Kafka.
 * Date: 2026-06-01
 */
public record PaymentCompletedEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        OffsetDateTime occurredAt,
        UUID reservationId,
        UUID paymentId,
        PaymentSimulationStatus status,
        BigDecimal amount
) {

    public static final String EVENT_TYPE = "PaymentCompletedEvent";

    public static PaymentCompletedEvent from(
            ReservationCreatedEvent reservationCreatedEvent,
            PaymentSimulationResult simulationResult
    ) {
        return new PaymentCompletedEvent(
                UUID.randomUUID(),
                EVENT_TYPE,
                reservationCreatedEvent.reservationId(),
                OffsetDateTime.now(),
                reservationCreatedEvent.reservationId(),
                simulationResult.paymentId(),
                simulationResult.status(),
                simulationResult.amount()
        );
    }
}