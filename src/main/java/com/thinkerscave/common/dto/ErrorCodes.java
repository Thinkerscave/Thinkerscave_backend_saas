package com.thinkerscave.common.dto;

/**
 * Centralized API response codes.
 *
 * <p>Used by {@link ApiResponse} and {@code GlobalExceptionHandler} to keep
 * error / success codes consistent across the platform.
 */
public final class ErrorCodes {

    private ErrorCodes() {}

    // ─── Success ──────────────────────────────────────────────────────────
    public static final String OK = "OK";
    public static final String CREATED = "CREATED";
    public static final String NO_CONTENT = "NO_CONTENT";

    // ─── Client Errors (4xx) ──────────────────────────────────────────────
    public static final String VALIDATION_ERROR     = "VALIDATION_ERROR";
    public static final String BAD_REQUEST          = "BAD_REQUEST";
    public static final String INVALID_ARGUMENT     = "INVALID_ARGUMENT";
    public static final String INVALID_CREDENTIALS  = "INVALID_CREDENTIALS";
    public static final String TOKEN_EXPIRED        = "TOKEN_EXPIRED";
    public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    public static final String ACCOUNT_LOCKED       = "ACCOUNT_LOCKED";
    public static final String ACCESS_DENIED        = "ACCESS_DENIED";
    public static final String FORBIDDEN            = "FORBIDDEN";
    public static final String NOT_FOUND            = "NOT_FOUND";
    public static final String CONFLICT             = "CONFLICT";
    public static final String BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION";
    public static final String RATE_LIMITED         = "RATE_LIMITED";

    // ─── Tenant / Organization ────────────────────────────────────────────
    public static final String TENANT_ALREADY_EXISTS = "TENANT_ALREADY_EXISTS";
    public static final String TENANT_ONBOARDING_FAILED = "TENANT_ONBOARDING_FAILED";
    public static final String SCHEMA_CREATION_FAILED = "SCHEMA_CREATION_FAILED";

    // ─── Server Errors (5xx) ──────────────────────────────────────────────
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
}
