package com.thinkerscave.common.exception;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle Invalid Login
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUsernameNotFound(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildError("Invalid Credentials", ex.getMessage(), HttpStatus.UNAUTHORIZED));
    }

    // Handle wrong password or login issues
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildError("Authentication Failed", ex.getMessage(), HttpStatus.UNAUTHORIZED));
    }

    // Handle JWT Expired
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<?> handleJwtExpired(ExpiredJwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildError("Token Expired", ex.getMessage(), HttpStatus.UNAUTHORIZED));
    }

    // Handle refresh token errors and others
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError("Error Occurred", ex.getMessage(), HttpStatus.BAD_REQUEST));
    }

    // Handle validation errors (e.g., @Valid failure)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest()
                .body(buildError("Validation Failed", "Input validation error", HttpStatus.BAD_REQUEST, fieldErrors));
    }

    // Fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
    	log.error("Unexpected Error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError("Internal Server Error", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
    }

    // Response structure
    private Map<String, Object> buildError(String error, String message, HttpStatus status) {
        return buildError(error, message, status, null);
    }

    private Map<String, Object> buildError(String error, String message, HttpStatus status, Map<String, String> fieldErrors) {
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("timestamp", LocalDateTime.now());
        errorMap.put("status", status.value());
        errorMap.put("error", error);
        errorMap.put("message", message);
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            errorMap.put("fieldErrors", fieldErrors);
        }
        return errorMap;
    }
}

