package com.thinkerscave.shared.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized API error response format.
 * 
 * All error responses in the application should use this format
 * for consistency across the API.
 * 
 * @author System
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class ApiError {

    /** HTTP status code */
    private int status;

    /** Short error code (e.g., "VALIDATION_ERROR", "NOT_FOUND") */
    private String code;

    /** Human-readable error message */
    private String message;

    /** Detailed error description (optional) */
    private String detail;

    /** Timestamp of the error */
    private LocalDateTime timestamp = LocalDateTime.now();

    /** Request path that caused the error */
    private String path;

    /** Correlation ID for tracking (optional) */
    private String correlationId;

    /** List of field-level validation errors (optional) */
    private List<FieldError> errors;

    /**
     * Field-level error for validation failures.
     */
    @Data
    @Builder
    public static class FieldError {
        /** Field that has the error */
        private String field;

        /** Error message for this field */
        private String message;

        /** Rejected value (optional) */
        private Object rejectedValue;
    }

}
