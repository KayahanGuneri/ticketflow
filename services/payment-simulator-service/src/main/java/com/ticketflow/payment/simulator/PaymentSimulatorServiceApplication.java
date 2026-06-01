package com.ticketflow.payment.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * @author Kayahan Güneri
 * Purpose: Starts the TicketFlow payment simulator service.
 * Date: 2026-06-01
 */
@ConfigurationPropertiesScan
@SpringBootApplication
public class PaymentSimulatorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentSimulatorServiceApplication.class, args);
    }
}