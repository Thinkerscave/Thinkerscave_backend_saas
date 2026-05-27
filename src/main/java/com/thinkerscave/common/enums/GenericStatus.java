package com.thinkerscave.common.enums;

/**
 * Generic activation/lifecycle status used by master-data and configuration
 * entities (e.g. {@code FeeHead}, {@code ExamType}, {@code Department}).
 *
 * <p>Use module-specific enums (e.g. {@code AdmissionStatus},
 * {@code InvoiceStatus}) when the lifecycle has more than three states.
 */
public enum GenericStatus {
    ACTIVE,
    INACTIVE,
    ARCHIVED
}
