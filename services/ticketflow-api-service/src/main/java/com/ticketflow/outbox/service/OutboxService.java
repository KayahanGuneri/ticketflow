package com.ticketflow.outbox.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.outbox.entity.OutboxEvent;
import com.ticketflow.outbox.entity.OutboxStatus;
import com.ticketflow.outbox.repository.OutboxRepository;
import com.ticketflow.reservation.entity.Reservation;
import com.ticketflow.reservation.event.ReservationCreatedEvent;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Kayahan Güneri
 * Purpose: Persists outbox events and updates their publishing state.
 * Date: 2026-05-30
 */
@Service
public class OutboxService {

    private static final String RESERVATION_AGGREGATE_TYPE = "RESERVATION";
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OutboxEvent saveReservationCreatedEvent(Reservation reservation) {
        ReservationCreatedEvent event = ReservationCreatedEvent.from(reservation);
        JsonNode payload = objectMapper.valueToTree(event);

        OutboxEvent outboxEvent = OutboxEvent.createPending(
                RESERVATION_AGGREGATE_TYPE,
                reservation.getId(),
                ReservationCreatedEvent.EVENT_TYPE,
                payload
        );

        return outboxRepository.save(outboxEvent);
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> findPendingEvents(int batchSize) {
        return outboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING,
                PageRequest.of(0, batchSize)
        );
    }

    @Transactional
    public void markAsPublished(OutboxEvent event) {
        event.markAsPublished();
        outboxRepository.save(event);
    }

    @Transactional
    public void markPublishAttemptFailed(OutboxEvent event, String errorMessage) {
        event.markPublishAttemptFailed(sanitizeErrorMessage(errorMessage));
        outboxRepository.save(event);
    }

    private String sanitizeErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "Unknown Kafka publishing error";
        }

        if (errorMessage.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return errorMessage;
        }

        return errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}