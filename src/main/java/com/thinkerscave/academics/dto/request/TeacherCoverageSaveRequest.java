package com.thinkerscave.academics.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Pending teacher-coverage changes for one academic group, saved together. */
@Getter
@Setter
public class TeacherCoverageSaveRequest {

    @NotNull(message = "Academic Group is mandatory")
    private Long academicGroupId;

    private List<@Valid Change> changes = new ArrayList<>();

    /** Required when the save changes teacher workload above capacity or changes timetable lessons. */
    private boolean confirmImpact;

    @Getter
    @Setter
    public static class Change {

        @NotNull(message = "Subject is mandatory")
        private Long mappingId;

        @NotNull(message = "Action is mandatory")
        private CoverageAction action;

        /** Teacher for ASSIGN_GROUP and ASSIGN_SECTIONS. */
        private Long staffId;

        /** Sections for ASSIGN_SECTIONS and CLEAR_SECTIONS. */
        private List<Long> sectionIds = new ArrayList<>();

        /** ASSIGN_GROUP only: when TRUE, existing section overrides are replaced; otherwise they are kept. */
        private boolean replaceOverrides;
    }

    public enum CoverageAction {
        ASSIGN_GROUP,
        CLEAR_GROUP,
        ASSIGN_SECTIONS,
        CLEAR_SECTIONS
    }
}
