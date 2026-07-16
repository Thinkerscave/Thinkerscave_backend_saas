package com.thinkerscave.dashboard.util;

import com.thinkerscave.access.enums.RoleType;

public final class RoleLabels {

    private RoleLabels() {}

    public static String of(RoleType type) {
        if (type == null) return "User";
        return switch (type) {
            case SUPER_ADMIN -> "Super Administrator";
            case ORGANIZATION_OWNER -> "Organization Owner";
            case ORGANIZATION_ADMIN -> "Organization Admin";
            case STAFF -> "Staff";
            case STUDENT -> "Student";
            case PARENT -> "Parent";
        };
    }

    public static String greeting() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }
}
