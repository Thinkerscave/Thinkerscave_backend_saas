package com.thinkerscave.common.course.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Lightweight aggregator DTOs for the Academics workspace dashboard.
 */
public final class AcademicsWorkspaceDtos {

    private AcademicsWorkspaceDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AcademicsKpi {
        private long classes;
        private long sections;
        private long subjects;
        private long teachersAssigned;
        private String activeYearCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassStructureCard {
        private Long classId;
        private String className;
        private int sectionCount;
        private int subjectCount;
        private int teacherCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AcademicsStructure {
        private List<ClassStructureCard> classes;
    }
}
