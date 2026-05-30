package com.ticketflow.kafka;

import com.ticketflow.reservation.event.ReservationCreatedEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Kayahan Güneri
 * Purpose: Verifies Kafka topic resolution for outbox event types.
 * Date: 2026-05-30
 */
class KafkaTopicResolverTest {

    @Test
    void shouldResolveReservationCreatedTopic() {
        KafkaTopicResolver resolver = new KafkaTopicResolver("ticket.reservation.created");

        String topic = resolver.resolveTopic(ReservationCreatedEvent.EVENT_TYPE);

        assertEquals("ticket.reservation.created", topic);
    }

    @Test
    void shouldRejectUnsupportedEventType() {
        KafkaTopicResolver resolver = new KafkaTopicResolver("ticket.reservation.created");

        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolveTopic("UnsupportedEvent")
        );
    }
}