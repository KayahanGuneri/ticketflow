package com.ticketflow.payment.simulator.common.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @author Kayahan Güneri
 * Purpose: Holds Kafka topic names used by the payment simulator service.
 * Date: 2026-06-01
 */
@Validated
@ConfigurationProperties(prefix = "ticketflow.kafka.topics")
public record KafkaTopicProperties(
        @NotBlank
        String reservationCreated,

        @NotBlank
        String paymentCompleted,

        @NotBlank
        String paymentFailed
) {
}