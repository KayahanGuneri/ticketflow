package com.ticketflow.reservation.service;

import com.ticketflow.common.exception.BusinessRuleViolationException;
import com.ticketflow.common.exception.DuplicateRequestException;
import com.ticketflow.common.exception.ResourceNotFoundException;
import com.ticketflow.event.entity.Event;
import com.ticketflow.event.repository.EventRepository;
import com.ticketflow.reservation.dto.CreateReservationRequest;
import com.ticketflow.reservation.dto.ReservationResponse;
import com.ticketflow.reservation.entity.Reservation;
import com.ticketflow.reservation.repository.ReservationRepository;
import com.ticketflow.ticket.entity.TicketInventory;
import com.ticketflow.ticket.repository.TicketInventoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * @author Kayahan Güneri
 * Purpose: Orchestrates transactional reservation creation, idempotency checks, and stock updates.
 * Date: 2026-05-29
 */
@Service
public class ReservationService {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 120;

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final TicketInventoryRepository ticketInventoryRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            EventRepository eventRepository,
            TicketInventoryRepository ticketInventoryRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.ticketInventoryRepository = ticketInventoryRepository;
    }

    /**
     * Creates a reservation and decreases ticket stock in the same transaction.
     * Idempotency is checked before mutating inventory to prevent duplicate stock decreases.
     */
    @Transactional
    public ReservationCreationResult createReservation(CreateReservationRequest request, String idempotencyKeyHeader) {
        String idempotencyKey = normalizeIdempotencyKey(idempotencyKeyHeader);

        return reservationRepository.findByIdempotencyKey(idempotencyKey)
                .map(existingReservation -> handleExistingReservation(existingReservation, request))
                .orElseGet(() -> createNewReservation(request, idempotencyKey));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservations() {
        return reservationRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(UUID id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));

        return toResponse(reservation);
    }

    private ReservationCreationResult createNewReservation(CreateReservationRequest request, String idempotencyKey) {
        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.eventId()));

        TicketInventory inventory = ticketInventoryRepository.findByEventId(request.eventId())
                .orElseThrow(() -> new BusinessRuleViolationException("Ticket inventory is missing for event: " + request.eventId()));

        inventory.reserve(request.ticketCount());

        Reservation reservation = Reservation.createPending(
                event,
                request.userId(),
                request.ticketCount(),
                idempotencyKey
        );

        Reservation savedReservation = reservationRepository.save(reservation);

        return new ReservationCreationResult(toResponse(savedReservation), true);
    }

    private ReservationCreationResult handleExistingReservation(Reservation existingReservation, CreateReservationRequest request) {
        boolean sameRequest =
                existingReservation.getEvent().getId().equals(request.eventId())
                        && existingReservation.getUserId().equals(request.userId())
                        && existingReservation.getTicketCount() == request.ticketCount();

        if (!sameRequest) {
            throw new DuplicateRequestException("Idempotency-Key is already used with a different reservation request");
        }

        return new ReservationCreationResult(toResponse(existingReservation), false);
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessRuleViolationException("Idempotency-Key header is required");
        }

        String normalizedKey = idempotencyKey.trim();

        if (normalizedKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new BusinessRuleViolationException("Idempotency-Key must not exceed 120 characters");
        }

        return normalizedKey;
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getEvent().getId(),
                reservation.getUserId(),
                reservation.getTicketCount(),
                reservation.getStatus(),
                reservation.getIdempotencyKey(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}