package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.AcademicYearStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class AcademicCalendarDashboardResponse {

    private Long academicYearId;
    private String name;
    private AcademicYearStatus status;
    private boolean yearReadOnly;

    private long eventCount;
    private long holidayCount;
    private long examinationCount;
    private long schoolEventCount;
    private long academicEventCount;
    private long otherCount;

    private List<AcademicCalendarEventResponse> upcoming;
    private List<AcademicCalendarEventResponse> events;
}
