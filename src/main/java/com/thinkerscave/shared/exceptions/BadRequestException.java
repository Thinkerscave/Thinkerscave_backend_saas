package com.thinkerscave.shared.exceptions;

/**
 * Client error for invalid requests. Optional {@code code} surfaces a stable
 * machine-readable reason (e.g. {@code ATTENDANCE_NOT_REQUIRED}).
 */
public class BadRequestException extends RuntimeException {

    private final String code;

    public BadRequestException(String message) {
        this("BAD_REQUEST", message);
    }

    public BadRequestException(String code, String message) {
        super(message);
        this.code = code != null && !code.isBlank() ? code : "BAD_REQUEST";
    }

    public String getCode() {
        return code;
    }
}
