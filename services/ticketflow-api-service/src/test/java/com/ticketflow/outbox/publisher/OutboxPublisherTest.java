package com.ticketflow.outbox.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.kafka.KafkaTopicResolver;
import com.ticketflow.outbox.entity.OutboxEvent;
import com.ticketflow.outbox.service.OutboxService;
import com.ticketflow.reservation.event.ReservationCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Kayahan Güneri
 * Purpose: Verifies Kafka publishing and outbox status update behavior.
 * Date: 2026-05-30
 */
@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    private static final String TOPIC = "ticket.reservation.created";
    private static final String PAYLOAD = "{\"eventType\":\"ReservationCreatedEvent\"}";

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private KafkaTopicResolver kafkaTopicResolver;

    @Mock
    private OutboxService outboxService;

    @Mock
    private ObjectMapper objectMapper;

    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        outboxPublisher = new OutboxPublisher(
                kafkaTemplate,
                kafkaTopicResolver,
                outboxService,
                objectMapper,
                5
        );
    }

    @Test
    void shouldPublishOutboxEventAndMarkAsPublished() throws Exception {
        OutboxEvent event = createOutboxEvent();

        when(kafkaTopicResolver.resolveTopic(ReservationCreatedEvent.EVENT_TYPE))
                .thenReturn(TOPIC);
        when(objectMapper.writeValueAsString(event.getPayload()))
                .thenReturn(PAYLOAD);

        CompletableFuture<SendResult<String, String>> successfulSend =
                CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(TOPIC, event.getAggregateId().toString(), PAYLOAD))
                .thenReturn(successfulSend);

        outboxPublisher.publish(event);

        verify(kafkaTemplate).send(TOPIC, event.getAggregateId().toString(), PAYLOAD);
        verify(outboxService).markAsPublished(event);
    }

    @Test
    void shouldKeepOutboxEventPendingWhenKafkaPublishFails() throws Exception {
        OutboxEvent event = createOutboxEvent();

        when(kafkaTopicResolver.resolveTopic(ReservationCreatedEvent.EVENT_TYPE))
                .thenReturn(TOPIC);
        when(objectMapper.writeValueAsString(event.getPayload()))
                .thenReturn(PAYLOAD);

        CompletableFuture<SendResult<String, String>> failedSend = new CompletableFuture<>();
        failedSend.completeExceptionally(new RuntimeException("Kafka unavailable"));

        when(kafkaTemplate.send(TOPIC, event.getAggregateId().toString(), PAYLOAD))
                .thenReturn(failedSend);

        outboxPublisher.publish(event);

        verify(kafkaTemplate).send(TOPIC, event.getAggregateId().toString(), PAYLOAD);
        verify(outboxService).markPublishAttemptFailed(eq(event), contains("Kafka unavailable"));
    }

    private OutboxEvent createOutboxEvent() {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode payload = mapper.createObjectNode()
                .put("eventType", ReservationCreatedEvent.EVENT_TYPE)
                .put("reservationId", UUID.randomUUID().toString());

        return OutboxEvent.createPending(
                "RESERVATION",
                UUID.randomUUID(),
                ReservationCreatedEvent.EVENT_TYPE,
                payload
        );
    }
}