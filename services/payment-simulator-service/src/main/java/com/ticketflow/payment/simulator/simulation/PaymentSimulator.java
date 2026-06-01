package com.ticketflow.payment.simulator.simulation;

import com.ticketflow.payment.simulator.payment.PaymentSimulationResult;
import com.ticketflow.payment.simulator.payment.ReservationCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;

/**
 * @author Kayahan Güneri
 * Purpose: Simulates an external payment provider response for a reservation.
 * Date: 2026-06-01
 */
@Service
public class PaymentSimulator {

    private static final String[] FAILURE_REASONS = {
            "SIMULATED_CARD_DECLINED",
            "SIMULATED_INSUFFICIENT_FUNDS",
            "SIMULATED_PROVIDER_TIMEOUT"
    };

    private final PaymentSimulationProperties properties;
    private final IntUnaryOperator randomIntGenerator;

    @Autowired
    public PaymentSimulator(PaymentSimulationProperties properties) {
        this(properties, ThreadLocalRandom.current()::nextInt);
    }

    PaymentSimulator(PaymentSimulationProperties properties, IntUnaryOperator randomIntGenerator) {
        this.properties = properties;
        this.randomIntGenerator = randomIntGenerator;
    }

    public PaymentSimulationResult simulate(ReservationCreatedEvent event) {
        if (event == null || event.reservationId() == null) {
            throw new IllegalArgumentException("ReservationCreatedEvent must contain reservationId");
        }

        simulateProviderDelay();

        int randomScore = nextRandomInt(100);

        if (randomScore < properties.successRate()) {
            return PaymentSimulationResult.completed(event.reservationId(), properties.defaultAmount());
        }

        return PaymentSimulationResult.failed(
                event.reservationId(),
                properties.defaultAmount(),
                randomFailureReason()
        );
    }

    private void simulateProviderDelay() {
        if (properties.simulatedDelayMs() == 0) {
            return;
        }

        try {
            Thread.sleep(properties.simulatedDelayMs());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Payment simulation was interrupted", exception);
        }
    }

    private String randomFailureReason() {
        return FAILURE_REASONS[nextRandomInt(FAILURE_REASONS.length)];
    }

    private int nextRandomInt(int bound) {
        int candidate = randomIntGenerator.applyAsInt(bound);
        return Math.floorMod(candidate, bound);
    }
}