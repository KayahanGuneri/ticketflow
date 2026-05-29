package com.ticketflow.common.exception;

/**
 * @author Kayahan Güneri
 * Purpose: Represents a business conflict when requested ticket count exceeds available stock.
 * Date: 2026-05-29
 */
public class InsufficientTicketCapacityException extends RuntimeException {

    public InsufficientTicketCapacityException(String message) {
        super(message);
    }
}