package com.ticketflow.reservation.service;

import com.ticketflow.common.exception.BusinessRuleViolationException;
import com.ticketflow.common.exception.DuplicateRequestException;
import com.ticketflow.common.exception.InsufficientTicketCapacityException;
import com.ticketflow.event.entity.Event;
import com.ticketflow.event.repository.EventRepository;
import com.ticketflow.outbox.service.OutboxService;
import com.ticketflow.reservation.dto.CreateReservationRequest;
import com.ticketflow.reservation.entity.Reservation;
import com.ticketflow.reservation.repository.ReservationRepository;
import com.ticketflow.ticket.entity.TicketInventory;
import com.ticketflow.ticket.repository.TicketInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Kayahan Güneri
 * Purpose: Verifies reservation transaction orchestration, idempotency behavior, stock consistency, and outbox persistence.
 * Date: 2026-05-29
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketInventoryRepository ticketInventoryRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private ReservationService reservationService;

    private UUID eventId;
    private UUID userId;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        userId = UUID.randomUUID();
        idempotencyKey = "reservation-test-key";
    }

    @Test
    void shouldCreateReservationWhenStockIsAvailable() {
        Event event = createEvent(eventId);
        TicketInventory inventory = TicketInventory.createForEvent(event, 5);
        CreateReservationRequest request = new CreateReservationRequest(eventId, userId, 2);

        when(reservationRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));
        when(ticketInventoryRepository.findByEventId(eventId))
                .thenReturn(Optional.of(inventory));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation reservation = invocation.getArgument(0);
                    ReflectionTestUtils.setField(reservation, "id", UUID.randomUUID());
                    return reservation;
                });

        ReservationCreationResult result = reservationService.createReservation(request, idempotencyKey);

        assertTrue(result.created());
        assertEquals(eventId, result.response().eventId());
        assertEquals(userId, result.response().userId());
        assertEquals(2, result.response().ticketCount());
        assertEquals(3, inventory.getAvailableCapacity());
        assertEquals(2, inventory.getReservedCapacity());

        verify(reservationRepository).save(any(Reservation.class));
        verify(outboxService).saveReservationCreatedEvent(any(Reservation.class));
    }

    @Test
    void shouldPersistOutboxEventWhenReservationIsCreated() {
        Event event = createEvent(eventId);
        TicketInventory inventory = TicketInventory.createForEvent(event, 5);
        CreateReservationRequest request = new CreateReservationRequest(eventId, userId, 2);

        when(reservationRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));
        when(ticketInventoryRepository.findByEventId(eventId))
                .thenReturn(Optional.of(inventory));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation reservation = invocation.getArgument(0);
                    ReflectionTestUtils.setField(reservation, "id", UUID.randomUUID());
                    return reservation;
                });

        reservationService.createReservation(request, idempotencyKey);

        verify(outboxService).saveReservationCreatedEvent(any(Reservation.class));
    }

    @Test
    void shouldReturnExistingReservationWhenIdempotencyKeyIsReusedWithSameRequest() {
        Event event = createEvent(eventId);
        Reservation existingReservation = Reservation.createPending(event, userId, 2, idempotencyKey);
        ReflectionTestUtils.setField(existingReservation, "id", UUID.randomUUID());

        CreateReservationRequest request = new CreateReservationRequest(eventId, userId, 2);

        when(reservationRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingReservation));

        ReservationCreationResult result = reservationService.createReservation(request, idempotencyKey);

        assertFalse(result.created());
        assertEquals(existingReservation.getId(), result.response().id());
        assertEquals(eventId, result.response().eventId());
        assertEquals(userId, result.response().userId());
        assertEquals(2, result.response().ticketCount());

        verify(eventRepository, never()).findById(any());
        verify(ticketInventoryRepository, never()).findByEventId(any());
        verify(reservationRepository, never()).save(any(Reservation.class));
        verify(outboxService, never()).saveReservationCreatedEvent(any(Reservation.class));
    }

    @Test
    void shouldRejectIdempotencyKeyReuseWhenRequestIsDifferent() {
        Event event = createEvent(eventId);
        Reservation existingReservation = Reservation.createPending(event, userId, 2, idempotencyKey);
        ReflectionTestUtils.setField(existingReservation, "id", UUID.randomUUID());

        CreateReservationRequest differentRequest = new CreateReservationRequest(eventId, userId, 1);

        when(reservationRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingReservation));

        assertThrows(
                DuplicateRequestException.class,
                () -> reservationService.createReservation(differentRequest, idempotencyKey)
        );

        verify(eventRepository, never()).findById(any());
        verify(ticketInventoryRepository, never()).findByEventId(any());
        verify(reservationRepository, never()).save(any(Reservation.class));
        verify(outboxService, never()).saveReservationCreatedEvent(any(Reservation.class));
    }

    @Test
    void shouldRejectReservationWhenStockIsInsufficient() {
        Event event = createEvent(eventId);
        TicketInventory inventory = TicketInventory.createForEvent(event, 1);
        CreateReservationRequest request = new CreateReservationRequest(eventId, userId, 2);

        when(reservationRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));
        when(ticketInventoryRepository.findByEventId(eventId))
                .thenReturn(Optional.of(inventory));

        assertThrows(
                InsufficientTicketCapacityException.class,
                () -> reservationService.createReservation(request, idempotencyKey)
        );

        assertEquals(1, inventory.getAvailableCapacity());
        assertEquals(0, inventory.getReservedCapacity());

        verify(reservationRepository, never()).save(any(Reservation.class));
        verify(outboxService, never()).saveReservationCreatedEvent(any(Reservation.class));
    }

    @Test
    void shouldRejectReservationWhenIdempotencyKeyIsBlank() {
        CreateReservationRequest request = new CreateReservationRequest(eventId, userId, 1);

        assertThrows(
                BusinessRuleViolationException.class,
                () -> reservationService.createReservation(request, " ")
        );

        verify(reservationRepository, never()).findByIdempotencyKey(any());
        verify(eventRepository, never()).findById(any());
        verify(ticketInventoryRepository, never()).findByEventId(any());
        verify(reservationRepository, never()).save(any(Reservation.class));
        verify(outboxService, never()).saveReservationCreatedEvent(any(Reservation.class));
    }

    private Event createEvent(UUID id) {
        Event event = Event.createDraft(
                "Backend Summit",
                "Test event",
                "Istanbul",
                OffsetDateTime.now().plusDays(30)
        );
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }
}