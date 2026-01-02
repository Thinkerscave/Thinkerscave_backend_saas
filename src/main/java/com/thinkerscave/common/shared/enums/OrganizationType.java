package com.thinkerscave.common.shared.enums;

/**
 * Organization types supported in the SaaS platform
 * Supports hierarchical multi-tenancy
 */
public enum OrganizationType {
    SCHOOL("School"),
    COLLEGE("College"),
    UNIVERSITY("University"),
    INSTITUTION("Institution"),
    GROUP("Group of Institutions"),
    TRAINING_CENTER("Training Center"),
    COACHING_CENTER("Coaching Center");

    private final String displayName;

    OrganizationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
