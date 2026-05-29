package com.ticketflow.reservation.entity;

/**
 * @author Kayahan Güneri
 * Purpose: Defines the lifecycle states of a ticket reservation.
 * Date: 2026-05-29
 */
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    EXPIRED
}