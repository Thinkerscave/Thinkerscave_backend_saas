package com.thinkerscave.common.enums;

/**
 * Blood group used by student/staff medical profiles.
 *
 * <p>Stored as the display string (e.g. {@code "O+"}) to keep DB rows human
 * readable; use {@link #fromLabel(String)} for parsing.
 */
public enum BloodGroup {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-"),
    UNKNOWN("Unknown");

    private final String label;

    BloodGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static BloodGroup fromLabel(String label) {
        if (label == null || label.isBlank()) return UNKNOWN;
        for (BloodGroup bg : values()) {
            if (bg.label.equalsIgnoreCase(label.trim())) return bg;
        }
        return UNKNOWN;
    }
}
