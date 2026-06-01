package com.ticketflow.payment.simulator.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.payment.simulator.common.config.KafkaTopicProperties;
import com.ticketflow.payment.simulator.payment.PaymentCompletedEvent;
import com.ticketflow.payment.simulator.payment.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Kayahan Güneri
 * Purpose: Publishes payment result events to Kafka.
 * Date: 2026-06-01
 */
@Component
public class PaymentResultProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicProperties kafkaTopicProperties;

    public PaymentResultProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            KafkaTopicProperties kafkaTopicProperties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.kafkaTopicProperties = kafkaTopicProperties;
    }

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        publish(kafkaTopicProperties.paymentCompleted(), event.reservationId().toString(), event);
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        publish(kafkaTopicProperties.paymentFailed(), event.reservationId().toString(), event);
    }

    private void publish(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(topic, key, payload)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.error(
                                    "Failed to publish payment result event. topic={}, key={}, error={}",
                                    topic,
                                    key,
                                    exception.getMessage()
                            );
                            return;
                        }

                        log.info("Published payment result event. topic={}, key={}", topic, key);
                    });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize payment result event", exception);
        }
    }
}