package com.thinkerscave.common.exception;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import com.thinkerscave.common.orgm.service.TenantOnboardingService.TenantAlreadyExistsException;
import com.thinkerscave.common.orgm.service.TenantOnboardingService.TenantOnboardingException;

import java.util.List;
import java.util.UUID;
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

        /**
         * Generates a unique correlation ID for error tracking.
         */
        private String generateCorrelationId() {
                return UUID.randomUUID().toString().substring(0, 8);
        }

        // ==================== Authentication Errors ====================

        @ExceptionHandler(UsernameNotFoundException.class)
        public ResponseEntity<ApiError> handleUsernameNotFound(UsernameNotFoundException ex,
                        HttpServletRequest request) {
                String correlationId = generateCorrelationId();
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
                String correlationId = generateCorrelationId();
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
                String correlationId = generateCorrelationId();
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
                String correlationId = generateCorrelationId();
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
                String correlationId = generateCorrelationId();
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
                String correlationId = generateCorrelationId();
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
                String correlationId = generateCorrelationId();
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
                String correlationId = generateCorrelationId();
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

        // ==================== Tenant/Organization Errors ====================

        @ExceptionHandler(TenantAlreadyExistsException.class)
        public ResponseEntity<ApiError> handleTenantAlreadyExists(TenantAlreadyExistsException ex,
                        HttpServletRequest request) {
                String correlationId = generateCorrelationId();
                log.warn("[{}] Tenant already exists: {}", correlationId, ex.getMessage());

                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiError.builder()
                                                .status(409)
                                                .code("TENANT_ALREADY_EXISTS")
                                                .message(ex.getMessage())
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        @ExceptionHandler(TenantOnboardingException.class)
        public ResponseEntity<ApiError> handleTenantOnboardingException(TenantOnboardingException ex,
                        HttpServletRequest request) {
                String correlationId = generateCorrelationId();
                log.error("[{}] Tenant onboarding failed: {}", correlationId, ex.getMessage(), ex);

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiError.builder()
                                                .status(500)
                                                .code("TENANT_ONBOARDING_FAILED")
                                                .message("Failed to onboard tenant. Please contact support.")
                                                .detail(ex.getMessage())
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        // ==================== Schema Errors ====================

        @ExceptionHandler(SchemaCreationException.class)
        public ResponseEntity<ApiError> handleSchemaCreationException(SchemaCreationException ex,
                        HttpServletRequest request) {
                String correlationId = generateCorrelationId();
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

        // ==================== Validation Errors ====================

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                String correlationId = generateCorrelationId();

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
                                                .message("Request validation failed")
                                                .errors(fieldErrors)
                                                .path(request.getRequestURI())
                                                .correlationId(correlationId)
                                                .build());
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
                String correlationId = generateCorrelationId();
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

        // ==================== Catch-All ====================

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiError> handleException(Exception ex, HttpServletRequest request) {
                String correlationId = generateCorrelationId();
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
