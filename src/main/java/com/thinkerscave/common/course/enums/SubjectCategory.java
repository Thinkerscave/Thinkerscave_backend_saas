package com.thinkerscave.common.course.enums;

/**
 * Subject category types
 */
public enum SubjectCategory {
    CORE("Core Subject"),
    ELECTIVE("Elective Subject"),
    LAB("Laboratory"),
    PRACTICAL("Practical"),
    THEORY("Theory"),
    PROJECT("Project Work"),
    INTERNSHIP("Internship");

    private final String displayName;

    SubjectCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
