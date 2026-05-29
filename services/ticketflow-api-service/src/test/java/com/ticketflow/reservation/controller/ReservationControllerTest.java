package com.ticketflow.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.reservation.dto.CreateReservationRequest;
import com.ticketflow.reservation.dto.ReservationResponse;
import com.ticketflow.reservation.entity.ReservationStatus;
import com.ticketflow.reservation.service.ReservationCreationResult;
import com.ticketflow.reservation.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Kayahan Güneri
 * Purpose: Verifies reservation API status codes for new creations and idempotent retries.
 * Date: 2026-05-29
 */
class ReservationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        reservationService = mock(ReservationService.class);
        ReservationController reservationController = new ReservationController(reservationService);

        mockMvc = MockMvcBuilders.standaloneSetup(reservationController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldReturnCreatedWhenReservationIsNew() throws Exception {
        UUID reservationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String idempotencyKey = "reservation-controller-key-001";

        CreateReservationRequest request = new CreateReservationRequest(eventId, userId, 1);
        ReservationResponse response = createReservationResponse(reservationId, eventId, userId, idempotencyKey);

        when(reservationService.createReservation(any(CreateReservationRequest.class), eq(idempotencyKey)))
                .thenReturn(new ReservationCreationResult(response, true));

        mockMvc.perform(post("/api/v1/reservations")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/reservations/" + reservationId))
                .andExpect(jsonPath("$.id", is(reservationId.toString())))
                .andExpect(jsonPath("$.eventId", is(eventId.toString())))
                .andExpect(jsonPath("$.userId", is(userId.toString())))
                .andExpect(jsonPath("$.ticketCount", is(1)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.idempotencyKey", is(idempotencyKey)));
    }

    @Test
    void shouldReturnOkWhenReservationIsIdempotentRetry() throws Exception {
        UUID reservationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String idempotencyKey = "reservation-controller-key-002";

        CreateReservationRequest request = new CreateReservationRequest(eventId, userId, 1);
        ReservationResponse response = createReservationResponse(reservationId, eventId, userId, idempotencyKey);

        when(reservationService.createReservation(any(CreateReservationRequest.class), eq(idempotencyKey)))
                .thenReturn(new ReservationCreationResult(response, false));

        mockMvc.perform(post("/api/v1/reservations")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(reservationId.toString())))
                .andExpect(jsonPath("$.eventId", is(eventId.toString())))
                .andExpect(jsonPath("$.userId", is(userId.toString())))
                .andExpect(jsonPath("$.ticketCount", is(1)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.idempotencyKey", is(idempotencyKey)));
    }

    private ReservationResponse createReservationResponse(
            UUID reservationId,
            UUID eventId,
            UUID userId,
            String idempotencyKey
    ) {
        OffsetDateTime now = OffsetDateTime.now();

        return new ReservationResponse(
                reservationId,
                eventId,
                userId,
                1,
                ReservationStatus.PENDING,
                idempotencyKey,
                now,
                now
        );
    }
}