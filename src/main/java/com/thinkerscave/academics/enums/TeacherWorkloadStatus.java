package com.thinkerscave.academics.enums;

/**
 * Workload classification for the Teacher Workload view.
 *
 * <p>Overloaded means the assigned periods exceed the provisional maximum. A teacher who
 * exceeds the estimated target but stays within the maximum is Near Maximum, never Overloaded.</p>
 */
public enum TeacherWorkloadStatus {
    OVERLOADED,
    NEAR_MAXIMUM,
    AT_TARGET,
    BELOW_TARGET,
    TARGET_UNAVAILABLE
}
