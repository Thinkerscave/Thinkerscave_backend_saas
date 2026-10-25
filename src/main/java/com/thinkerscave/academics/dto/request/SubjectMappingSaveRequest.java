package com.thinkerscave.academics.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Batch of curriculum changes for one academic group, saved atomically. */
@Getter
@Setter
public class SubjectMappingSaveRequest {

    @NotNull(message = "Academic year is mandatory")
    private Long academicYearId;

    /** Adds and edits. Each entry replaces the stored settings of that subject's mapping. */
    private List<@Valid SubjectMappingEntryRequest> upserts = new ArrayList<>();

    /** Subjects whose mapping in this group is deactivated. The catalog subject is never touched. */
    private List<Long> removedSubjectIds = new ArrayList<>();

    /** Required when the change removes or re-schedules mappings that have teacher or timetable dependencies. */
    private boolean confirmImpact;
}
