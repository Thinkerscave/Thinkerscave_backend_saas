package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.TimetableEntryType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TimetableCellResponse {

    private DayOfWeek dayOfWeek;
    private Long periodId;
    private Short periodNumber;
    private Long entryId;
    private TimetableEntryType entryType;
    private Long sectionId;
    private String sectionName;
    private String academicGroupName;
    private Long subjectId;
    private String subjectName;
    private Long academicGroupSubjectMappingId;
    private Long staffId;
    private String staffName;
    private Long teacherAllocationId;
    private Long fixedActivityId;
    private String fixedActivityType;
    private Long resourceId;
    private String resourceName;
    private Boolean locked;
    private java.time.LocalTime startTime;
    private java.time.LocalTime endTime;
}
