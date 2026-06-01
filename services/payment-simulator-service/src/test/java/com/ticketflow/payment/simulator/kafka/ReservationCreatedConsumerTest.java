package com.ticketflow.payment.simulator.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.payment.simulator.payment.PaymentCompletedEvent;
import com.ticketflow.payment.simulator.payment.PaymentFailedEvent;
import com.ticketflow.payment.simulator.payment.PaymentSimulationResult;
import com.ticketflow.payment.simulator.payment.ReservationCreatedEvent;
import com.ticketflow.payment.simulator.simulation.PaymentSimulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationCreatedConsumerTest {

    @Mock
    private PaymentSimulator paymentSimulator;

    @Mock
    private PaymentResultProducer paymentResultProducer;

    private ObjectMapper objectMapper;
    private ReservationCreatedConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        consumer = new ReservationCreatedConsumer(objectMapper, paymentSimulator, paymentResultProducer);
    }

    @Test
    void shouldPublishCompletedEventWhenPaymentSimulationSucceeds() throws Exception {
        ReservationCreatedEvent event = validReservationCreatedEvent();
        PaymentSimulationResult result = PaymentSimulationResult.completed(
                event.reservationId(),
                new BigDecimal("100.00")
        );

        when(paymentSimulator.simulate(any(ReservationCreatedEvent.class))).thenReturn(result);

        consumer.consume(objectMapper.writeValueAsString(event));

        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);

        verify(paymentResultProducer).publishPaymentCompleted(eventCaptor.capture());
        verify(paymentResultProducer, never()).publishPaymentFailed(any());

        assertThat(eventCaptor.getValue().reservationId()).isEqualTo(event.reservationId());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo(PaymentCompletedEvent.EVENT_TYPE);
    }

    @Test
    void shouldPublishFailedEventWhenPaymentSimulationFails() throws Exception {
        ReservationCreatedEvent event = validReservationCreatedEvent();
        PaymentSimulationResult result = PaymentSimulationResult.failed(
                event.reservationId(),
                new BigDecimal("100.00"),
                "SIMULATED_CARD_DECLINED"
        );

        when(paymentSimulator.simulate(any(ReservationCreatedEvent.class))).thenReturn(result);

        consumer.consume(objectMapper.writeValueAsString(event));

        ArgumentCaptor<PaymentFailedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentFailedEvent.class);

        verify(paymentResultProducer).publishPaymentFailed(eventCaptor.capture());
        verify(paymentResultProducer, never()).publishPaymentCompleted(any());

        assertThat(eventCaptor.getValue().reservationId()).isEqualTo(event.reservationId());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo(PaymentFailedEvent.EVENT_TYPE);
        assertThat(eventCaptor.getValue().failureReason()).isEqualTo("SIMULATED_CARD_DECLINED");
    }

    @Test
    void shouldLogAndSkipWhenEventIsInvalid() throws Exception {
        ReservationCreatedEvent event = new ReservationCreatedEvent(
                UUID.randomUUID(),
                "UnknownEvent",
                UUID.randomUUID(),
                OffsetDateTime.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                2
        );

        consumer.consume(objectMapper.writeValueAsString(event));

        verify(paymentSimulator, never()).simulate(any());
        verify(paymentResultProducer, never()).publishPaymentCompleted(any());
        verify(paymentResultProducer, never()).publishPaymentFailed(any());
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