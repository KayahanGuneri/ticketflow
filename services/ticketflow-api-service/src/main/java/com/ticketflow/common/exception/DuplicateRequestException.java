package com.ticketflow.common.exception;

/**
 * @author Kayahan Güneri
 * Purpose: Represents an idempotency conflict when the same key is reused with different request data.
 * Date: 2026-05-29
 */
public class DuplicateRequestException extends RuntimeException {

    public DuplicateRequestException(String message) {
        super(message);
    }
}