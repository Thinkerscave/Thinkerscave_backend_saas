package com.thinkerscave.common.enums;

/**
 * Gender values used across student, staff and parent profiles.
 *
 * <p>{@code OTHER} and {@code PREFER_NOT_TO_SAY} are kept distinct so reports
 * can distinguish "self-identified other" from "declined to answer".
 */
public enum Gender {
    MALE,
    FEMALE,
    OTHER,
    PREFER_NOT_TO_SAY
}
