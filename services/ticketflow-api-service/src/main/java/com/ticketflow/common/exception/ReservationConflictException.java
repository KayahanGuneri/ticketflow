package com.ticketflow.common.exception;

/**
 * @author Kayahan Güneri
 * Purpose: Represents reservation conflicts caused by concurrent updates or invalid reservation state.
 * Date: 2026-05-29
 */
public class ReservationConflictException extends RuntimeException {

    public ReservationConflictException(String message) {
        super(message);
    }
}