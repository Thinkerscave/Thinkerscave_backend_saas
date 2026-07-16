package com.thinkerscave.dashboard.enums;

/**
 * Identifies which fixed, typed dashboard layout a user should receive.
 * Mirrors {@link com.thinkerscave.access.enums.RoleType} 1:1, plus a
 * {@code DEFAULT} fallback for users whose role cannot be resolved to a
 * dedicated experience.
 */
public enum DashboardType {

    SUPER_ADMIN,

    ORG_OWNER,

    ORG_ADMIN,

    STAFF,

    STUDENT,

    PARENT,

    DEFAULT
}
