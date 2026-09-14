package com.thinkerscave.shared.exceptions;

/**
 * Same idempotency key reused with a different request body hash.
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
