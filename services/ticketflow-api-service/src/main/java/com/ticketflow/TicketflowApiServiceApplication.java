package com.ticketflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TicketflowApiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketflowApiServiceApplication.class, args);
    }
}