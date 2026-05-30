package com.ticketflow.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * @author Kayahan Güneri
 * Purpose: Defines Kafka topics required by the TicketFlow API service.
 * Date: 2026-05-30
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic reservationCreatedTopic(
            @Value("${ticketflow.kafka.topics.reservation-created}") String topicName
    ) {
        return TopicBuilder.name(topicName)
                .partitions(1)
                .replicas(1)
                .build();
    }
}