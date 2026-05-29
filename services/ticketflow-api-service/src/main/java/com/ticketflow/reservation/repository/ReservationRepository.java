package com.ticketflow.reservation.repository;

import com.ticketflow.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Kayahan Güneri
 * Purpose: Provides persistence operations for reservation records.
 * Date: 2026-05-29
 */
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByIdempotencyKey(String idempotencyKey);
}