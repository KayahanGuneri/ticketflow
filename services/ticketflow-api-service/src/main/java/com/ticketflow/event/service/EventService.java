package com.ticketflow.event.service;

import com.ticketflow.common.exception.BusinessRuleViolationException;
import com.ticketflow.common.exception.ResourceNotFoundException;
import com.ticketflow.event.dto.CreateEventRequest;
import com.ticketflow.event.dto.EventResponse;
import com.ticketflow.event.entity.Event;
import com.ticketflow.event.repository.EventRepository;
import com.ticketflow.ticket.entity.TicketInventory;
import com.ticketflow.ticket.repository.TicketInventoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final TicketInventoryRepository ticketInventoryRepository;

    public EventService(
            EventRepository eventRepository,
            TicketInventoryRepository ticketInventoryRepository
    ) {
        this.eventRepository = eventRepository;
        this.ticketInventoryRepository = ticketInventoryRepository;
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        Event event = Event.createDraft(
                normalizeRequiredText(request.name()),
                normalizeOptionalText(request.description()),
                normalizeRequiredText(request.location()),
                request.startsAt()
        );

        Event savedEvent = eventRepository.save(event);

        TicketInventory inventory = TicketInventory.createForEvent(
                savedEvent,
                request.totalCapacity()
        );

        TicketInventory savedInventory = ticketInventoryRepository.save(inventory);

        return toResponse(savedEvent, savedInventory);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getEvents() {
        List<Event> events = eventRepository.findAll(Sort.by(Sort.Direction.ASC, "startsAt"));

        if (events.isEmpty()) {
            return List.of();
        }

        List<UUID> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        Map<UUID, TicketInventory> inventoryByEventId = ticketInventoryRepository.findAllByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        inventory -> inventory.getEvent().getId(),
                        Function.identity()
                ));

        return events.stream()
                .map(event -> {
                    TicketInventory inventory = inventoryByEventId.get(event.getId());

                    if (inventory == null) {
                        throw new BusinessRuleViolationException("Ticket inventory is missing for event: " + event.getId());
                    }

                    return toResponse(event, inventory);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));

        TicketInventory inventory = ticketInventoryRepository.findByEventId(id)
                .orElseThrow(() -> new BusinessRuleViolationException("Ticket inventory is missing for event: " + id));

        return toResponse(event, inventory);
    }

    private EventResponse toResponse(Event event, TicketInventory inventory) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getLocation(),
                event.getStartsAt(),
                event.getStatus(),
                inventory.getTotalCapacity(),
                inventory.getAvailableCapacity(),
                inventory.getReservedCapacity(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    private String normalizeRequiredText(String value) {
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}