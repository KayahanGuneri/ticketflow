package com.ticketflow.payment.simulator.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * @author Kayahan Güneri
 * Purpose: Defines Kafka topics produced by the payment simulator service.
 * Date: 2026-06-01
 */
@Configuration
public class KafkaTopicConfig {

    private final KafkaTopicProperties kafkaTopicProperties;

    public KafkaTopicConfig(KafkaTopicProperties kafkaTopicProperties) {
        this.kafkaTopicProperties = kafkaTopicProperties;
    }

    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder.name(kafkaTopicProperties.paymentCompleted())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(kafkaTopicProperties.paymentFailed())
                .partitions(1)
                .replicas(1)
                .build();
    }
}