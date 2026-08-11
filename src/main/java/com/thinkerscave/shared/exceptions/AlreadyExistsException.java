package com.thinkerscave.shared.exceptions;

/**
 * Thrown when a create/update would violate a uniqueness business rule.
 * Optional {@code field} lets the API attach a field-level error for UI binding.
 */
public class AlreadyExistsException extends RuntimeException {

    private final String field;

    public AlreadyExistsException(String message) {
        this(message, null);
    }

    public AlreadyExistsException(String message, String field) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
