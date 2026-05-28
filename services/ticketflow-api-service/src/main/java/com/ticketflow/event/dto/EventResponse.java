package com.ticketflow.event.dto;

import com.ticketflow.event.entity.EventStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String name,
        String description,
        String location,
        OffsetDateTime startsAt,
        EventStatus status,
        int totalCapacity,
        int availableCapacity,
        int reservedCapacity,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}