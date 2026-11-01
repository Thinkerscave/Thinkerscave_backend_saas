package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Live counts for one Academic Group. {@code timetableStatus} is year-level because timetable
 * versions are generated per academic year, not per group.
 */
@Getter
@Builder
public class AcademicGroupSummaryResponse {
    private Long academicGroupId;
    private long sectionCount;
    private long activeSectionCount;
    private long studentCount;
    private long subjectCount;
    private long teacherCount;
    /** PUBLISHED, DRAFT or NOT_GENERATED for the academic year. */
    private String timetableStatus;
}
