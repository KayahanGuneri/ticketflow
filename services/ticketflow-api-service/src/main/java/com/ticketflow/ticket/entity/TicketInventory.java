package com.ticketflow.ticket.entity;

import com.ticketflow.common.exception.InsufficientTicketCapacityException;
import com.ticketflow.event.entity.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * @author Kayahan Güneri
 * Purpose: Represents ticket inventory for an event and protects stock consistency with optimistic locking.
 * Date: 2026-05-29
 */
@Entity
@Table(name = "ticket_inventories")
public class TicketInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @Column(name = "total_capacity", nullable = false)
    private int totalCapacity;

    @Column(name = "available_capacity", nullable = false)
    private int availableCapacity;

    @Column(name = "reserved_capacity", nullable = false)
    private int reservedCapacity;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected TicketInventory() {
    }

    private TicketInventory(Event event, int totalCapacity) {
        this.event = event;
        this.totalCapacity = totalCapacity;
        this.availableCapacity = totalCapacity;
        this.reservedCapacity = 0;
    }

    public static TicketInventory createForEvent(Event event, int totalCapacity) {
        return new TicketInventory(event, totalCapacity);
    }

    /**
     * Reserves tickets by moving stock from available capacity to reserved capacity.
     * The @Version field protects this state change against lost updates.
     */
    public void reserve(int ticketCount) {
        if (ticketCount <= 0) {
            throw new IllegalArgumentException("Ticket count must be positive");
        }

        if (availableCapacity < ticketCount) {
            throw new InsufficientTicketCapacityException(
                    "Insufficient ticket capacity. Requested: " + ticketCount + ", available: " + availableCapacity
            );
        }

        this.availableCapacity -= ticketCount;
        this.reservedCapacity += ticketCount;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public int getAvailableCapacity() {
        return availableCapacity;
    }

    public int getReservedCapacity() {
        return reservedCapacity;
    }

    public long getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}