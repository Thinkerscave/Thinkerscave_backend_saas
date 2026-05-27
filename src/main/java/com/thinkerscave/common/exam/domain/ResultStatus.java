package com.thinkerscave.common.exam.domain;

/** Final outcome of a student's exam {@link Result}. */
public enum ResultStatus {
    PENDING,
    PASS,
    FAIL,
    ABSENT,
    EXEMPTED,
    DEBARRED,
    WITHHELD,
    RE_EXAM_REQUIRED
}
