package com.ticketflow.ticket.repository;

import com.ticketflow.ticket.entity.TicketInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketInventoryRepository extends JpaRepository<TicketInventory, UUID> {

    @Query("select inventory from TicketInventory inventory join fetch inventory.event event where event.id = :eventId")
    Optional<TicketInventory> findByEventId(@Param("eventId") UUID eventId);

    @Query("select inventory from TicketInventory inventory join fetch inventory.event event where event.id in :eventIds")
    List<TicketInventory> findAllByEventIds(@Param("eventIds") Collection<UUID> eventIds);
}