package com.ticketflow.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.common.exception.GlobalExceptionHandler;
import com.ticketflow.common.exception.ResourceNotFoundException;
import com.ticketflow.event.dto.CreateEventRequest;
import com.ticketflow.event.dto.EventResponse;
import com.ticketflow.event.entity.EventStatus;
import com.ticketflow.event.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventControllerTest {

    private EventService eventService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        eventService = mock(EventService.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();

        EventController eventController = new EventController(eventService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(eventController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void shouldCreateEventWhenRequestIsValid() throws Exception {
        UUID eventId = UUID.randomUUID();
        OffsetDateTime startsAt = OffsetDateTime.now().plusDays(10);

        CreateEventRequest request = new CreateEventRequest(
                "Spring Boot Architecture Workshop",
                "Backend-focused workshop",
                "Istanbul",
                startsAt,
                100
        );

        EventResponse response = new EventResponse(
                eventId,
                request.name(),
                request.description(),
                request.location(),
                request.startsAt(),
                EventStatus.DRAFT,
                100,
                100,
                0,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(eventService.createEvent(any(CreateEventRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.name").value("Spring Boot Architecture Workshop"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalCapacity").value(100))
                .andExpect(jsonPath("$.availableCapacity").value(100))
                .andExpect(jsonPath("$.reservedCapacity").value(0));
    }

    @Test
    void shouldRejectCreateEventWhenNameIsBlank() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "",
                "Backend-focused workshop",
                "Istanbul",
                OffsetDateTime.now().plusDays(10),
                100
        );

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.name").value("Event name is required"));

        verifyNoInteractions(eventService);
    }

    @Test
    void shouldReturnEventsWhenEventsExist() throws Exception {
        UUID eventId = UUID.randomUUID();

        EventResponse response = new EventResponse(
                eventId,
                "Java Backend Day",
                "Backend event",
                "Ankara",
                OffsetDateTime.now().plusDays(5),
                EventStatus.DRAFT,
                50,
                50,
                0,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(eventService.getEvents()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(eventId.toString()))
                .andExpect(jsonPath("$[0].name").value("Java Backend Day"));
    }

    @Test
    void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
        UUID eventId = UUID.randomUUID();

        when(eventService.getEventById(eventId))
                .thenThrow(new ResourceNotFoundException("Event not found: " + eventId));

        mockMvc.perform(get("/api/v1/events/{id}", eventId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Event not found: " + eventId));
    }
}