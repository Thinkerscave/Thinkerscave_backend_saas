package com.thinkerscave.shared.exceptions;


import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.thinkerscave.shared.constants.ErrorCodes;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Global exception handler for standardized API error responses.
 * 
 * All exceptions are converted to ApiError format with:
 * - Consistent structure
 * - Correlation IDs for tracking
 * - Appropriate HTTP status codes
 * - Detailed error information
 * 
 * @author System
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        // ==================== Authentication Errors ====================

        @ExceptionHandler(UsernameNotFoundException.class)
        public ResponseEntity<ApiError> handleUsernameNotFound(UsernameNotFoundException ex,
                        HttpServletRequest request) {
                String correlationId = MDC.get("correlationId");
                log.warn("[{}] Username not found: {}", correlationId, ex.getMessage());

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiError.builder()
                                                .status(401)
                                                .code("INVALID_CREDENTIALS")
                                                .message("Invalid username or password")
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        @ExceptionHandler(org.springframework.security.authentication.LockedException.class)
        public ResponseEntity<ApiError> handleLockedException(
                        org.springframework.security.authentication.LockedException ex, HttpServletRequest request) {
                String correlationId = MDC.get("correlationId");
                log.warn("[{}] Account locked: {}", correlationId, ex.getMessage());

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ApiError.builder()
                                                .status(403)
                                                .code("ACCOUNT_LOCKED")
                                                .message(ex.getMessage())
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
                String correlationId = MDC.get("correlationId");
                log.warn("[{}] Bad credentials: {}", correlationId, ex.getMessage());

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiError.builder()
                                                .status(401)
                                                .code("INVALID_CREDENTIALS")
                                                .message("Invalid username or password")
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        @ExceptionHandler(ExpiredJwtException.class)
        public ResponseEntity<ApiError> handleJwtExpired(ExpiredJwtException ex, HttpServletRequest request) {
                String correlationId = MDC.get("correlationId");
                log.warn("[{}] JWT token expired", correlationId);

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiError.builder()
                                                .status(401)
                                                .code("TOKEN_EXPIRED")
                                                .message("Your session has expired. Please log in again.")
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex,
                        HttpServletRequest request) {
                String correlationId = MDC.get("correlationId");
                log.warn("[{}] Authentication failed: {}", correlationId, ex.getMessage());

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiError.builder()
                                                .status(401)
                                                .code("AUTHENTICATION_FAILED")
                                                .message("Authentication failed")
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
                String correlationId = MDC.get("correlationId");
                log.warn("[{}] Access denied: {}", correlationId, ex.getMessage());

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ApiError.builder()
                                                .status(403)
                                                .code("ACCESS_DENIED")
                                                .message("You do not have permission to access this resource")
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        // ==================== Resource Errors ====================

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex,
                        HttpServletRequest request) {
                String correlationId = MDC.get("correlationId");
                log.info("[{}] Resource not found: {}", correlationId, ex.getMessage());

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiError.builder()
                                                .status(404)
                                                .code("NOT_FOUND")
                                                .message(ex.getMessage())
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
                String correlationId = MDC.get("correlationId");
                log.info("[{}] Bad request: {}", correlationId, ex.getMessage());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiError.builder()
                                                .status(400)
                                                .code("BAD_REQUEST")
                                                .message(ex.getMessage())
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        // ==================== Schema Errors ====================

        @ExceptionHandler(SchemaCreationException.class)
        public ResponseEntity<ApiError> handleSchemaCreationException(SchemaCreationException ex,
                        HttpServletRequest request) {
                String correlationId = MDC.get("correlationId");
                log.error("[{}] Schema creation failed: {}", correlationId, ex.getMessage(), ex);

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiError.builder()
                                                .status(500)
                                                .code("SCHEMA_CREATION_FAILED")
                                                .message("Failed to create tenant schema. Please contact support.")
                                                .detail(ex.getMessage())
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ApiError> handleBusinessException(
                BusinessException ex,
                HttpServletRequest request) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiError.builder()
                            .status(400)
                            .code(ErrorCodes.BUSINESS_RULE_VIOLATION)
                            .message(ex.getMessage())
                            .path(request.getRequestURI())
                            .build());
        }
        
        @ExceptionHandler(AlreadyExistsException.class)
        public ResponseEntity<ApiError> handleAlreadyExists(
                AlreadyExistsException ex,
                HttpServletRequest request) {
            String correlationId = MDC.get("correlationId");
            List<ApiError.FieldError> fieldErrors = null;
            if (ex.getField() != null && !ex.getField().isBlank()) {
                fieldErrors = List.of(ApiError.FieldError.builder()
                        .field(ex.getField())
                        .message(ex.getMessage())
                        .build());
            }

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiError.builder()
                            .status(409)
                            .code(ErrorCodes.ALREADY_EXISTS)
                            .message(ex.getMessage())
                            .errors(fieldErrors)
                            .path(request.getRequestURI())
                            .correlationId(correlationId)
                            .build());
        }
        
        @ExceptionHandler(FileProcessingException.class)
        public ResponseEntity<ApiError> handleFileProcessing(
                FileProcessingException ex,
                HttpServletRequest request) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.builder()
                            .status(500)
                            .code(ErrorCodes.FILE_PROCESSING_ERROR)
                            .message(ex.getMessage())
                            .path(request.getRequestURI())
                            .build());
        }
        // ==================== Validation Errors ====================

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                String correlationId = MDC.get("correlationId");

                List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                                .map(error -> ApiError.FieldError.builder()
                                                .field(error.getField())
                                                .message(error.getDefaultMessage())
                                                .rejectedValue(error.getRejectedValue())
                                                .build())
                                .collect(Collectors.toList());

                log.info("[{}] Validation failed with {} errors", correlationId, fieldErrors.size());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiError.builder()
                                                .status(400)
                                                .code("VALIDATION_FAILED")
                                                .message(buildValidationSummary(fieldErrors))
                                                .errors(fieldErrors)
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        private String buildValidationSummary(List<ApiError.FieldError> fieldErrors) {
                if (fieldErrors == null || fieldErrors.isEmpty()) {
                        return "Request validation failed";
                }
                ApiError.FieldError first = fieldErrors.get(0);
                String field = first.getField() != null ? first.getField() : "field";
                String msg = first.getMessage() != null ? first.getMessage() : "is invalid";
                if (fieldErrors.size() == 1) {
                        return field + ": " + msg;
                }
                return field + ": " + msg + " (and " + (fieldErrors.size() - 1) + " more)";
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
                String correlationId = MDC.get("correlationId");
                log.warn("[{}] Illegal argument: {}", correlationId, ex.getMessage());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiError.builder()
                                                .status(400)
                                                .code("INVALID_ARGUMENT")
                                                .message(ex.getMessage())
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        // ==================== Data & Type Errors ====================

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                        HttpServletRequest request) {
                String correlationId = MDC.get("correlationId");

                List<ApiError.FieldError> fieldErrors = ex.getConstraintViolations().stream()
                                .map(v -> ApiError.FieldError.builder()
                                                .field(v.getPropertyPath().toString())
                                                .message(v.getMessage())
                                                .rejectedValue(v.getInvalidValue())
                                                .build())
                                .collect(Collectors.toList());

                log.info("[{}] Constraint violation with {} errors", correlationId, fieldErrors.size());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiError.builder()
                                                .status(400)
                                                .code("CONSTRAINT_VIOLATION")
                                                .message("Validation constraint violation")
                                                .errors(fieldErrors)
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                        HttpServletRequest request) {
                String correlationId = MDC.get("correlationId");
                String cause = ex.getMostSpecificCause() != null
                                ? ex.getMostSpecificCause().getMessage()
                                : ex.getMessage();
                log.warn("[{}] Data integrity violation: {}", correlationId, cause);

                String message = "A data conflict occurred. The record may already exist or references invalid data.";
                String field = null;
                String lower = cause != null ? cause.toLowerCase(Locale.ROOT) : "";
                if (lower.contains("tenant_identifier") || lower.contains("schema_name")
                                || lower.contains("sub_domain") || lower.contains("organization_domains")
                                || lower.contains("tenant_registry")) {
                        message = "This domain is already in use. Choose another domain.";
                        field = "tenantSubdomain";
                } else if (lower.contains("email") || lower.contains("username")) {
                        message = "This email or username is already in use.";
                        field = "adminEmail";
                }

                List<ApiError.FieldError> fieldErrors = field == null ? null : List.of(
                                ApiError.FieldError.builder().field(field).message(message).build());

                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiError.builder()
                                                .status(409)
                                                .code("DATA_INTEGRITY_VIOLATION")
                                                .message(message)
                                                .errors(fieldErrors)
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                        HttpServletRequest request) {
                String correlationId = MDC.get("correlationId");
                String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
                log.info("[{}] Type mismatch for parameter '{}': expected {}", correlationId, ex.getName(), expectedType);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiError.builder()
                                                .status(400)
                                                .code("TYPE_MISMATCH")
                                                .message(String.format("Parameter '%s' should be of type '%s'",
                                                                ex.getName(), expectedType))
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                        HttpServletRequest request) {
                String correlationId = MDC.get("correlationId");
                log.info("[{}] Method not supported: {} for {}", correlationId, ex.getMethod(),
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                                .body(ApiError.builder()
                                                .status(405)
                                                .code("METHOD_NOT_ALLOWED")
                                                .message(String.format("HTTP method '%s' is not supported for this endpoint",
                                                                ex.getMethod()))
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
        public ResponseEntity<ApiError> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                        HttpServletRequest request) {
                String correlationId = MDC.get("correlationId");
                log.info("[{}] Media type not supported: {}", correlationId, ex.getContentType());

                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                                .body(ApiError.builder()
                                                .status(415)
                                                .code("UNSUPPORTED_MEDIA_TYPE")
                                                .message(String.format("Content type '%s' is not supported",
                                                                ex.getContentType()))
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        @ExceptionHandler(MissingServletRequestParameterException.class)
        public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex,
                        HttpServletRequest request) {
                String correlationId = MDC.get("correlationId");
                log.info("[{}] Missing required parameter: '{}'", correlationId, ex.getParameterName());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiError.builder()
                                                .status(400)
                                                .code("MISSING_PARAMETER")
                                                .message(String.format("Required parameter '%s' of type '%s' is missing",
                                                                ex.getParameterName(), ex.getParameterType()))
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        // ==================== Catch-All ====================

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiError> handleException(Exception ex, HttpServletRequest request) {
                String correlationId = MDC.get("correlationId");
                log.error("[{}] Unhandled exception at {}: {}", correlationId, request.getRequestURI(), ex.getMessage(),
                                ex);

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiError.builder()
                                                .status(500)
                                                .code("INTERNAL_ERROR")
                                                .message("An unexpected error occurred. Please try again or contact support.")
                                                .detail("Error ID: " + correlationId)
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }
        
        
}
