package com.thinkerscave.common.course.enums;

/**
 * Container types for different institution structures
 * - CLASS: For schools (Class 10-A, Class 12-B)
 * - BRANCH: For colleges (CS Branch, Mechanical Branch)
 * - DEPARTMENT: For universities (Computer Science Dept, Physics Dept)
 * - MODULE: For training centers (Module 1, Module 2)
 * - BATCH: For training centers (Batch 2024-Jan, Batch 2024-Feb)
 * - SECTION: For subdivisions (Section A, Section B)
 * - YEAR: For year-based programs (Year 1, Year 2)
 */
public enum ContainerType {
    CLASS("Class"),
    BRANCH("Branch"),
    DEPARTMENT("Department"),
    MODULE("Module"),
    BATCH("Batch"),
    SECTION("Section"),
    YEAR("Year");

    private final String displayName;

    ContainerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
