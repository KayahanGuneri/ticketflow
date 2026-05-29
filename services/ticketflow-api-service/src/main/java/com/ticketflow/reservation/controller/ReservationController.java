package com.ticketflow.reservation.controller;

import com.ticketflow.reservation.dto.CreateReservationRequest;
import com.ticketflow.reservation.dto.ReservationResponse;
import com.ticketflow.reservation.service.ReservationCreationResult;
import com.ticketflow.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * @author Kayahan Güneri
 * Purpose: Exposes reservation APIs for creating and reading ticket reservations.
 * Date: 2026-05-29
 */
@RestController
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservations", description = "Ticket reservation APIs")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Create reservation", description = "Creates a reservation with idempotency and transactional stock decrease.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reservation created successfully"),
            @ApiResponse(responseCode = "200", description = "Existing reservation returned for idempotent retry"),
            @ApiResponse(responseCode = "400", description = "Invalid reservation request"),
            @ApiResponse(responseCode = "404", description = "Event not found"),
            @ApiResponse(responseCode = "409", description = "Reservation conflict or insufficient stock")
    })
    public ResponseEntity<ReservationResponse> createReservation(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateReservationRequest request
    ) {
        ReservationCreationResult result = reservationService.createReservation(request, idempotencyKey);
        URI location = URI.create("/api/v1/reservations/" + result.response().id());

        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .location(location)
                    .body(result.response());
        }

        return ResponseEntity.ok(result.response());
    }

    @GetMapping
    @Operation(summary = "List reservations", description = "Returns all reservations ordered by creation time descending.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservations returned successfully")
    })
    public ResponseEntity<List<ReservationResponse>> getReservations() {
        return ResponseEntity.ok(reservationService.getReservations());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reservation by id", description = "Returns a single reservation by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation returned successfully"),
            @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }
}