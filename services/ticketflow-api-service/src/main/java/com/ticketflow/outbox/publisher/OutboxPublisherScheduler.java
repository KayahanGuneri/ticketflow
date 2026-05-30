package com.ticketflow.outbox.publisher;

import com.ticketflow.outbox.entity.OutboxEvent;
import com.ticketflow.outbox.service.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Kayahan Güneri
 * Purpose: Polls pending outbox events and delegates Kafka publishing.
 * Date: 2026-05-30
 */
@Component
public class OutboxPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherScheduler.class);

    private final OutboxService outboxService;
    private final OutboxPublisher outboxPublisher;
    private final int batchSize;

    public OutboxPublisherScheduler(
            OutboxService outboxService,
            OutboxPublisher outboxPublisher,
            @Value("${ticketflow.outbox.publisher.batch-size:20}") int batchSize
    ) {
        this.outboxService = outboxService;
        this.outboxPublisher = outboxPublisher;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${ticketflow.outbox.publisher.fixed-delay-ms:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxService.findPendingEvents(batchSize);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending outbox event(s) ready for Kafka publishing", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            outboxPublisher.publish(event);
        }
    }
}