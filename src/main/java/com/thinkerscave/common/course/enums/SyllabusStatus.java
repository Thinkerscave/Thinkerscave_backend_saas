package com.thinkerscave.common.course.enums;

/**
 * Syllabus lifecycle status
 */
public enum SyllabusStatus {
    DRAFT("Draft - Work in Progress"),
    PENDING_APPROVAL("Pending Approval"),
    APPROVED("Approved - Ready to Publish"),
    PUBLISHED("Published - Active"),
    ARCHIVED("Archived - Historical");

    private final String displayName;

    SyllabusStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
