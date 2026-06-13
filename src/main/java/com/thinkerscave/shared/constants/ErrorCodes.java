package com.thinkerscave.shared.constants;

public final class ErrorCodes {

    private ErrorCodes() {}

    // Success
    public static final String OK = "OK";
    public static final String CREATED = "CREATED";
    public static final String NO_CONTENT = "NO_CONTENT";

    // Client Errors
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String INVALID_ARGUMENT = "INVALID_ARGUMENT";

    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String FORBIDDEN = "FORBIDDEN";

    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";
    public static final String ALREADY_EXISTS = "ALREADY_EXISTS";
    public static final String DUPLICATE_RECORD = "DUPLICATE_RECORD";

    public static final String BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION";

    public static final String IMPORT_VALIDATION_ERROR = "IMPORT_VALIDATION_ERROR";

    // File Errors
    public static final String FILE_UPLOAD_ERROR = "FILE_UPLOAD_ERROR";
    public static final String FILE_PROCESSING_ERROR = "FILE_PROCESSING_ERROR";

    // Persistence
    public static final String DATA_INTEGRITY_ERROR = "DATA_INTEGRITY_ERROR";

    // Server Errors
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
}