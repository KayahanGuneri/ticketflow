package com.ticketflow.payment.simulator.simulation;

import com.ticketflow.payment.simulator.payment.PaymentSimulationResult;
import com.ticketflow.payment.simulator.payment.PaymentSimulationStatus;
import com.ticketflow.payment.simulator.payment.ReservationCreatedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentSimulatorTest {

    @Test
    void shouldReturnCompletedResultWhenRandomValueIsWithinSuccessRate() {
        PaymentSimulationProperties properties = new PaymentSimulationProperties(
                80,
                0,
                new BigDecimal("100.00")
        );
        PaymentSimulator simulator = new PaymentSimulator(properties, bound -> 10);

        PaymentSimulationResult result = simulator.simulate(validReservationCreatedEvent());

        assertThat(result.status()).isEqualTo(PaymentSimulationStatus.COMPLETED);
        assertThat(result.paymentId()).isNotNull();
        assertThat(result.failureReason()).isNull();
        assertThat(result.amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldReturnFailedResultWhenRandomValueExceedsSuccessRate() {
        PaymentSimulationProperties properties = new PaymentSimulationProperties(
                80,
                0,
                new BigDecimal("100.00")
        );
        PaymentSimulator simulator = new PaymentSimulator(properties, bound -> 90);

        PaymentSimulationResult result = simulator.simulate(validReservationCreatedEvent());

        assertThat(result.status()).isEqualTo(PaymentSimulationStatus.FAILED);
        assertThat(result.paymentId()).isNotNull();
        assertThat(result.failureReason()).isNotBlank();
        assertThat(result.amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldRejectInvalidSuccessRateBelowZero() {
        assertThatThrownBy(() -> new PaymentSimulationProperties(
                -1,
                0,
                new BigDecimal("100.00")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("payment-simulator.success-rate must be between 0 and 100");
    }

    @Test
    void shouldRejectInvalidSuccessRateAboveOneHundred() {
        assertThatThrownBy(() -> new PaymentSimulationProperties(
                101,
                0,
                new BigDecimal("100.00")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("payment-simulator.success-rate must be between 0 and 100");
    }

    private ReservationCreatedEvent validReservationCreatedEvent() {
        UUID reservationId = UUID.randomUUID();

        return new ReservationCreatedEvent(
                UUID.randomUUID(),
                ReservationCreatedEvent.EVENT_TYPE,
                reservationId,
                OffsetDateTime.now(),
                reservationId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                2
        );
    }
}