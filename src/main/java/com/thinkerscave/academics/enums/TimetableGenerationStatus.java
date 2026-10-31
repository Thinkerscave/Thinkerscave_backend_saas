package com.thinkerscave.academics.enums;

public enum TimetableGenerationStatus {
    NOT_GENERATED,
    GENERATING,
    GENERATED,
    GENERATED_WITH_CONFLICTS,
    /** Draft created for manual entry without running the automatic generator. */
    MANUAL,
    FAILED
}
