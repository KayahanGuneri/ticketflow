package com.ticketflow;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketflowApiServiceApplicationTests {

    @Test
    void shouldCreateApplicationInstance() {
        assertThat(new TicketflowApiServiceApplication()).isNotNull();
    }
}