package com.ticketflow.payment.simulator.simulation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * @author Kayahan Güneri
 * Purpose: Holds configurable rules for payment simulation.
 * Date: 2026-06-01
 */
@ConfigurationProperties(prefix = "payment-simulator")
public record PaymentSimulationProperties(
        int successRate,
        long simulatedDelayMs,
        BigDecimal defaultAmount
) {

    public PaymentSimulationProperties {
        if (successRate < 0 || successRate > 100) {
            throw new IllegalArgumentException("payment-simulator.success-rate must be between 0 and 100");
        }

        if (simulatedDelayMs < 0) {
            throw new IllegalArgumentException("payment-simulator.simulated-delay-ms must not be negative");
        }

        if (defaultAmount == null || defaultAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("payment-simulator.default-amount must not be negative");
        }
    }
}