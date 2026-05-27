package com.thinkerscave.common.exam.domain;

/** Lifecycle of an {@link Exam}. */
public enum ExamStatus {
    PLANNED,
    SCHEDULED,
    IN_PROGRESS,
    MARKS_ENTRY,
    EVALUATION,
    RESULT_DECLARED,
    ARCHIVED,
    CANCELLED
}
