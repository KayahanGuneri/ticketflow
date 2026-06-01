package com.ticketflow.payment.simulator.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Kayahan Güneri
 * Purpose: Represents the internal result of a simulated payment attempt.
 * Date: 2026-06-01
 */
public record PaymentSimulationResult(
        UUID reservationId,
        UUID paymentId,
        PaymentSimulationStatus status,
        BigDecimal amount,
        String failureReason
) {

    public static PaymentSimulationResult completed(UUID reservationId, BigDecimal amount) {
        return new PaymentSimulationResult(
                reservationId,
                UUID.randomUUID(),
                PaymentSimulationStatus.COMPLETED,
                amount,
                null
        );
    }

    public static PaymentSimulationResult failed(UUID reservationId, BigDecimal amount, String failureReason) {
        return new PaymentSimulationResult(
                reservationId,
                UUID.randomUUID(),
                PaymentSimulationStatus.FAILED,
                amount,
                failureReason
        );
    }

    public boolean isCompleted() {
        return PaymentSimulationStatus.COMPLETED.equals(status);
    }

    public boolean isFailed() {
        return PaymentSimulationStatus.FAILED.equals(status);
    }
}