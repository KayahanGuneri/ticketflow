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

import java.util.List;

@Service
public class OutboxService {

    private static final String RESERVATION_AGGREGATE_TYPE = "RESERVATION";

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

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

    public List<OutboxEvent> findPendingEvents(int batchSize) {
        return outboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING,
                PageRequest.of(0, batchSize)
        );
    }
}