package com.ticketflow.payment.simulator.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.payment.simulator.common.config.KafkaTopicProperties;
import com.ticketflow.payment.simulator.payment.PaymentCompletedEvent;
import com.ticketflow.payment.simulator.payment.PaymentFailedEvent;
import com.ticketflow.payment.simulator.payment.PaymentSimulationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentResultProducerTest {

    private static final String PAYMENT_COMPLETED_TOPIC = "ticket.payment.completed";
    private static final String PAYMENT_FAILED_TOPIC = "ticket.payment.failed";

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;
    private PaymentResultProducer producer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();

        KafkaTopicProperties kafkaTopicProperties = new KafkaTopicProperties(
                "ticket.reservation.created",
                PAYMENT_COMPLETED_TOPIC,
                PAYMENT_FAILED_TOPIC
        );

        producer = new PaymentResultProducer(kafkaTemplate, objectMapper, kafkaTopicProperties);
    }

    @Test
    void shouldPublishPaymentCompletedEventToCompletedTopic() throws Exception {
        UUID reservationId = UUID.randomUUID();

        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(),
                PaymentCompletedEvent.EVENT_TYPE,
                reservationId,
                OffsetDateTime.now(),
                reservationId,
                UUID.randomUUID(),
                PaymentSimulationStatus.COMPLETED,
                new BigDecimal("100.00")
        );

        when(kafkaTemplate.send(eq(PAYMENT_COMPLETED_TOPIC), eq(reservationId.toString()), anyString()))
                .thenReturn(completedSend());

        producer.publishPaymentCompleted(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate).send(
                eq(PAYMENT_COMPLETED_TOPIC),
                eq(reservationId.toString()),
                payloadCaptor.capture()
        );

        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());

        assertThat(payload.get("eventType").asText()).isEqualTo(PaymentCompletedEvent.EVENT_TYPE);
        assertThat(payload.get("reservationId").asText()).isEqualTo(reservationId.toString());
        assertThat(payload.get("status").asText()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldPublishPaymentFailedEventToFailedTopic() throws Exception {
        UUID reservationId = UUID.randomUUID();

        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(),
                PaymentFailedEvent.EVENT_TYPE,
                reservationId,
                OffsetDateTime.now(),
                reservationId,
                UUID.randomUUID(),
                PaymentSimulationStatus.FAILED,
                new BigDecimal("100.00"),
                "SIMULATED_CARD_DECLINED"
        );

        when(kafkaTemplate.send(eq(PAYMENT_FAILED_TOPIC), eq(reservationId.toString()), anyString()))
                .thenReturn(completedSend());

        producer.publishPaymentFailed(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate).send(
                eq(PAYMENT_FAILED_TOPIC),
                eq(reservationId.toString()),
                payloadCaptor.capture()
        );

        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());

        assertThat(payload.get("eventType").asText()).isEqualTo(PaymentFailedEvent.EVENT_TYPE);
        assertThat(payload.get("reservationId").asText()).isEqualTo(reservationId.toString());
        assertThat(payload.get("status").asText()).isEqualTo("FAILED");
        assertThat(payload.get("failureReason").asText()).isEqualTo("SIMULATED_CARD_DECLINED");
    }

    private CompletableFuture<SendResult<String, String>> completedSend() {
        return CompletableFuture.completedFuture(null);
    }
}