package com.ticketflow.event.service;

import com.ticketflow.common.exception.ResourceNotFoundException;
import com.ticketflow.event.dto.CreateEventRequest;
import com.ticketflow.event.dto.EventResponse;
import com.ticketflow.event.entity.Event;
import com.ticketflow.event.entity.EventStatus;
import com.ticketflow.event.repository.EventRepository;
import com.ticketflow.ticket.entity.TicketInventory;
import com.ticketflow.ticket.repository.TicketInventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketInventoryRepository ticketInventoryRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void shouldCreateEventWhenRequestIsValid() {
        UUID eventId = UUID.randomUUID();
        OffsetDateTime startsAt = OffsetDateTime.now().plusDays(10);

        CreateEventRequest request = new CreateEventRequest(
                " Spring Boot Architecture Workshop ",
                " Backend-focused workshop ",
                " Istanbul ",
                startsAt,
                100
        );

        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            ReflectionTestUtils.setField(event, "id", eventId);
            ReflectionTestUtils.setField(event, "createdAt", OffsetDateTime.now());
            ReflectionTestUtils.setField(event, "updatedAt", OffsetDateTime.now());
            return event;
        });

        when(ticketInventoryRepository.save(any(TicketInventory.class))).thenAnswer(invocation -> {
            TicketInventory inventory = invocation.getArgument(0);
            ReflectionTestUtils.setField(inventory, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(inventory, "createdAt", OffsetDateTime.now());
            ReflectionTestUtils.setField(inventory, "updatedAt", OffsetDateTime.now());
            return inventory;
        });

        EventResponse response = eventService.createEvent(request);

        assertThat(response.id()).isEqualTo(eventId);
        assertThat(response.name()).isEqualTo("Spring Boot Architecture Workshop");
        assertThat(response.description()).isEqualTo("Backend-focused workshop");
        assertThat(response.location()).isEqualTo("Istanbul");
        assertThat(response.startsAt()).isEqualTo(startsAt);
        assertThat(response.status()).isEqualTo(EventStatus.DRAFT);
        assertThat(response.totalCapacity()).isEqualTo(100);
        assertThat(response.availableCapacity()).isEqualTo(100);
        assertThat(response.reservedCapacity()).isZero();

        verify(eventRepository).save(any(Event.class));
        verify(ticketInventoryRepository).save(any(TicketInventory.class));
    }

    @Test
    void shouldReturnEventByIdWhenEventExists() {
        UUID eventId = UUID.randomUUID();
        OffsetDateTime startsAt = OffsetDateTime.now().plusDays(5);

        Event event = Event.createDraft(
                "Java Backend Day",
                "Backend event",
                "Ankara",
                startsAt
        );

        ReflectionTestUtils.setField(event, "id", eventId);
        ReflectionTestUtils.setField(event, "createdAt", OffsetDateTime.now());
        ReflectionTestUtils.setField(event, "updatedAt", OffsetDateTime.now());

        TicketInventory inventory = TicketInventory.createForEvent(event, 50);

        ReflectionTestUtils.setField(inventory, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(inventory, "createdAt", OffsetDateTime.now());
        ReflectionTestUtils.setField(inventory, "updatedAt", OffsetDateTime.now());

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(ticketInventoryRepository.findByEventId(eventId)).thenReturn(Optional.of(inventory));

        EventResponse response = eventService.getEventById(eventId);

        assertThat(response.id()).isEqualTo(eventId);
        assertThat(response.name()).isEqualTo("Java Backend Day");
        assertThat(response.totalCapacity()).isEqualTo(50);
        assertThat(response.availableCapacity()).isEqualTo(50);
        assertThat(response.reservedCapacity()).isZero();
    }

    @Test
    void shouldThrowResourceNotFoundWhenEventDoesNotExist() {
        UUID eventId = UUID.randomUUID();

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventById(eventId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found: " + eventId);
    }
}