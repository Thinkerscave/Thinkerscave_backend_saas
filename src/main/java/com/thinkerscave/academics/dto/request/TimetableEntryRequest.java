package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.DayOfWeek;
import lombok.Getter;
import lombok.Setter;

/** Manual timetable edit: place/move a lesson or substitute its teacher. */
@Getter
@Setter
public class TimetableEntryRequest {

    private Long sectionId;
    private Long academicGroupSubjectMappingId;
    private Long teacherAllocationId;
    private Long staffId;
    private DayOfWeek dayOfWeek;
    private Long periodId;
    private Boolean locked;
}
