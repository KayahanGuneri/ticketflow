package com.ticketflow.payment.simulator.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.payment.simulator.payment.PaymentCompletedEvent;
import com.ticketflow.payment.simulator.payment.PaymentFailedEvent;
import com.ticketflow.payment.simulator.payment.PaymentSimulationResult;
import com.ticketflow.payment.simulator.payment.ReservationCreatedEvent;
import com.ticketflow.payment.simulator.simulation.PaymentSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @author Kayahan Güneri
 * Purpose: Consumes reservation-created events and starts payment simulation.
 * Date: 2026-06-01
 */
@Component
public class ReservationCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReservationCreatedConsumer.class);

    private final ObjectMapper objectMapper;
    private final PaymentSimulator paymentSimulator;
    private final PaymentResultProducer paymentResultProducer;

    public ReservationCreatedConsumer(
            ObjectMapper objectMapper,
            PaymentSimulator paymentSimulator,
            PaymentResultProducer paymentResultProducer
    ) {
        this.objectMapper = objectMapper;
        this.paymentSimulator = paymentSimulator;
        this.paymentResultProducer = paymentResultProducer;
    }

    @KafkaListener(topics = "${ticketflow.kafka.topics.reservation-created}")
    public void consume(String payload) {
        try {
            ReservationCreatedEvent event = objectMapper.readValue(payload, ReservationCreatedEvent.class);

            if (!event.isProcessable()) {
                // Invalid reservation events are skipped to avoid blocking the consumer with poison messages.
                log.warn("Skipped invalid reservation created event. eventId={}, eventType={}", event.eventId(), event.eventType());
                return;
            }

            log.info("Received reservation created event. reservationId={}", event.reservationId());

            PaymentSimulationResult simulationResult = paymentSimulator.simulate(event);

            if (simulationResult.isCompleted()) {
                PaymentCompletedEvent completedEvent = PaymentCompletedEvent.from(event, simulationResult);
                paymentResultProducer.publishPaymentCompleted(completedEvent);

                log.info(
                        "Payment simulation completed successfully. reservationId={}, paymentId={}",
                        simulationResult.reservationId(),
                        simulationResult.paymentId()
                );
                return;
            }

            PaymentFailedEvent failedEvent = PaymentFailedEvent.from(event, simulationResult);
            paymentResultProducer.publishPaymentFailed(failedEvent);

            log.info(
                    "Payment simulation failed. reservationId={}, paymentId={}, reason={}",
                    simulationResult.reservationId(),
                    simulationResult.paymentId(),
                    simulationResult.failureReason()
            );
        } catch (JsonProcessingException exception) {
            // Malformed JSON is skipped because retrying the same invalid payload would keep failing.
            log.error("Failed to deserialize reservation created event. error={}", exception.getMessage());
        } catch (RuntimeException exception) {
            log.error("Failed to process reservation created event. error={}", exception.getMessage(), exception);
            throw exception;
        }
    }
}