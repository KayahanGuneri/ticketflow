package com.ticketflow.ticket.entity;

import com.ticketflow.common.exception.InsufficientTicketCapacityException;
import com.ticketflow.event.entity.Event;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Kayahan Güneri
 * Purpose: Verifies ticket inventory stock behavior.
 * Date: 2026-05-29
 */
class TicketInventoryTest {

    @Test
    void shouldReserveTicketsWhenCapacityIsAvailable() {
        Event event = Event.createDraft(
                "Backend Summit",
                "Test event",
                "Istanbul",
                OffsetDateTime.now().plusDays(30)
        );
        TicketInventory inventory = TicketInventory.createForEvent(event, 5);

        inventory.reserve(2);

        assertEquals(3, inventory.getAvailableCapacity());
        assertEquals(2, inventory.getReservedCapacity());
        assertEquals(5, inventory.getTotalCapacity());
    }

    @Test
    void shouldRejectReservationWhenCapacityIsInsufficient() {
        Event event = Event.createDraft(
                "Backend Summit",
                "Test event",
                "Istanbul",
                OffsetDateTime.now().plusDays(30)
        );
        TicketInventory inventory = TicketInventory.createForEvent(event, 1);

        assertThrows(
                InsufficientTicketCapacityException.class,
                () -> inventory.reserve(2)
        );

        assertEquals(1, inventory.getAvailableCapacity());
        assertEquals(0, inventory.getReservedCapacity());
    }

    @Test
    void shouldRejectReservationWhenTicketCountIsNotPositive() {
        Event event = Event.createDraft(
                "Backend Summit",
                "Test event",
                "Istanbul",
                OffsetDateTime.now().plusDays(30)
        );
        TicketInventory inventory = TicketInventory.createForEvent(event, 5);

        assertThrows(
                IllegalArgumentException.class,
                () -> inventory.reserve(0)
        );

        assertEquals(5, inventory.getAvailableCapacity());
        assertEquals(0, inventory.getReservedCapacity());
    }
}