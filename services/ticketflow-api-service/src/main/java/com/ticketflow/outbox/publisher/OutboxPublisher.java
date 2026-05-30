package com.ticketflow.outbox.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.kafka.KafkaTopicResolver;
import com.ticketflow.outbox.entity.OutboxEvent;
import com.ticketflow.outbox.service.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Kayahan Güneri
 * Purpose: Publishes outbox events to Kafka and updates their delivery status.
 * Date: 2026-05-30
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicResolver kafkaTopicResolver;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;
    private final long publishTimeoutSeconds;

    public OutboxPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaTopicResolver kafkaTopicResolver,
            OutboxService outboxService,
            ObjectMapper objectMapper,
            @Value("${ticketflow.outbox.publisher.publish-timeout-seconds:10}") long publishTimeoutSeconds
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTopicResolver = kafkaTopicResolver;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
        this.publishTimeoutSeconds = publishTimeoutSeconds;
    }

    public void publish(OutboxEvent event) {
        try {
            String topic = kafkaTopicResolver.resolveTopic(event.getEventType());
            String key = event.getAggregateId().toString();
            String payload = objectMapper.writeValueAsString(event.getPayload());

            kafkaTemplate.send(topic, key, payload)
                    .get(publishTimeoutSeconds, TimeUnit.SECONDS);

            outboxService.markAsPublished(event);

            log.info(
                    "Published outbox event to Kafka: id={}, topic={}, eventType={}, aggregateId={}",
                    event.getId(),
                    topic,
                    event.getEventType(),
                    event.getAggregateId()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            handlePublishFailure(event, exception);
        } catch (JsonProcessingException | ExecutionException | TimeoutException | RuntimeException exception) {
            handlePublishFailure(event, exception);
        }
    }

    private void handlePublishFailure(OutboxEvent event, Exception exception) {
        // The event must not be lost; keeping it PENDING allows the scheduler to retry later.
        outboxService.markPublishAttemptFailed(event, exception.getMessage());

        log.warn(
                "Failed to publish outbox event: id={}, eventType={}, aggregateId={}, error={}",
                event.getId(),
                event.getEventType(),
                event.getAggregateId(),
                exception.getMessage()
        );
    }
}