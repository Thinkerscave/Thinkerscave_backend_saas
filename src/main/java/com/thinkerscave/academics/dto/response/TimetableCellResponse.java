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
    private String className;
    private String subjectName;
    private Long staffId;
    private String staffName;
    private Long resourceId;
    private String resourceName;
}
