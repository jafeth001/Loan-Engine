package com.bank.loanengine.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Structured error response returned for all API errors")
public record ErrorResponse(

        @Schema(description = "Timestamp of the error", example = "2024-06-01T12:00:00")
        LocalDateTime timestamp,

        @Schema(description = "HTTP status code", example = "422")
        int status,

        @Schema(description = "Short error category", example = "Unprocessable Entity")
        String error,

        @Schema(description = "Human-readable error detail", example = "Prepayment amount exceeds outstanding principal")
        String message,

        @Schema(description = "Field-level validation errors (only present on 400 responses)",
                nullable = true)
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, null);
    }

    public static ErrorResponse of(int status, String error, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, fieldErrors);
    }
}
