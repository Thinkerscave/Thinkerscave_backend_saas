package com.thinkerscave.common.course.enums;

/**
 * Progress status for topic/chapter completion tracking
 */
public enum ProgressStatus {
    NOT_STARTED("Not Started"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    SKIPPED("Skipped");

    private final String displayName;

    ProgressStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
