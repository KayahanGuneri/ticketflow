package com.ticketflow.common.response;

import java.time.OffsetDateTime;
import java.util.Map;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(
                OffsetDateTime.now(),
                status,
                error,
                message,
                path,
                Map.of()
        );
    }

    public static ApiErrorResponse validation(String message, String path, Map<String, String> validationErrors) {
        return new ApiErrorResponse(
                OffsetDateTime.now(),
                400,
                "Bad Request",
                message,
                path,
                validationErrors
        );
    }
}