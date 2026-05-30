package com.ticketflow.kafka;

import com.ticketflow.reservation.event.ReservationCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Kayahan Güneri
 * Purpose: Maps outbox event types to Kafka topics.
 * Date: 2026-05-30
 */
@Component
public class KafkaTopicResolver {

    private final String reservationCreatedTopic;

    public KafkaTopicResolver(
            @Value("${ticketflow.kafka.topics.reservation-created}") String reservationCreatedTopic
    ) {
        this.reservationCreatedTopic = reservationCreatedTopic;
    }

    public String resolveTopic(String eventType) {
        if (ReservationCreatedEvent.EVENT_TYPE.equals(eventType)) {
            return reservationCreatedTopic;
        }

        throw new IllegalArgumentException("Unsupported outbox event type: " + eventType);
    }
}