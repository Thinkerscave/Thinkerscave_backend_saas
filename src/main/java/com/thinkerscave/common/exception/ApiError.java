package com.thinkerscave.common.exception;

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

    // Convenience factory methods

    public static ApiError of(int status, String code, String message) {
        return ApiError.builder()
                .status(status)
                .code(code)
                .message(message)
                .build();
    }

    public static ApiError of(int status, String code, String message, String path) {
        return ApiError.builder()
                .status(status)
                .code(code)
                .message(message)
                .path(path)
                .build();
    }

    public static ApiError notFound(String resource, Object id) {
        return ApiError.builder()
                .status(404)
                .code("NOT_FOUND")
                .message(String.format("%s with ID '%s' not found", resource, id))
                .build();
    }

    public static ApiError validationError(String message, List<FieldError> errors) {
        return ApiError.builder()
                .status(400)
                .code("VALIDATION_ERROR")
                .message(message)
                .errors(errors)
                .build();
    }

    public static ApiError unauthorized(String message) {
        return ApiError.builder()
                .status(401)
                .code("UNAUTHORIZED")
                .message(message)
                .build();
    }

    public static ApiError forbidden(String message) {
        return ApiError.builder()
                .status(403)
                .code("FORBIDDEN")
                .message(message)
                .build();
    }

    public static ApiError internalError(String message) {
        return ApiError.builder()
                .status(500)
                .code("INTERNAL_ERROR")
                .message(message)
                .build();
    }

    public static ApiError conflict(String message) {
        return ApiError.builder()
                .status(409)
                .code("CONFLICT")
                .message(message)
                .build();
    }

    public static ApiError badRequest(String message) {
        return ApiError.builder()
                .status(400)
                .code("BAD_REQUEST")
                .message(message)
                .build();
    }
}
