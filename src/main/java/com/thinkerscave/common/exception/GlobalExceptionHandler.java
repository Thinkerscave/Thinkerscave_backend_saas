package com.thinkerscave.common.exception;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import com.thinkerscave.common.dto.ApiResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle Invalid Login
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUsernameNotFound(UsernameNotFoundException ex) {
        return buildResponse(false, "Invalid Credentials: " + ex.getMessage(), null, HttpStatus.UNAUTHORIZED);
    }

    // Handle wrong password or login issues
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
        return buildResponse(false, "Authentication Failed: " + ex.getMessage(), null, HttpStatus.UNAUTHORIZED);
    }

    // Handle JWT Expired
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Object>> handleJwtExpired(ExpiredJwtException ex) {
        return buildResponse(false, "Token Expired: " + ex.getMessage(), null, HttpStatus.UNAUTHORIZED);
    }

    // Handle Generic Authentication Exceptions (Locked, Disabled, etc.)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(AuthenticationException ex) {
        return buildResponse(false, "Authentication Failed: " + ex.getMessage(), null, HttpStatus.UNAUTHORIZED);
    }

    // Handle Resource Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildResponse(false, ex.getMessage(), null, HttpStatus.NOT_FOUND);
    }

    // Handle Bad Request
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(BadRequestException ex) {
        return buildResponse(false, ex.getMessage(), null, HttpStatus.BAD_REQUEST);
    }

    // Handle Schema Creation Error
    @ExceptionHandler(SchemaCreationException.class)
    public ResponseEntity<ApiResponse<Object>> handleSchemaCreationException(SchemaCreationException ex) {
        return buildResponse(false, "Schema Creation Failed: " + ex.getMessage(), null,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Handle validation errors (e.g., @Valid failure)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        return buildResponse(false, "Validation Failed", fieldErrors, HttpStatus.BAD_REQUEST);
    }

    // 🔥 Handles ALL unhandled exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
        return buildResponse(false, "Internal Server Error: " + ex.getMessage(), null,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiResponse<Object>> buildResponse(boolean success, String message, Object data,
            HttpStatus status) {
        ApiResponse<Object> response = ApiResponse.builder()
                .success(success)
                .message(message)
                .data(data)
                .build();
        return new ResponseEntity<>(response, status);
    }

}
