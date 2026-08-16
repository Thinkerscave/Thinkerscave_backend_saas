package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.TimetableConflictStatus;
import com.thinkerscave.academics.enums.TimetableConflictType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class TimetableConflictResponse {

    private Long timetableConflictId;
    private Long timetableVersionId;
    private TimetableConflictType conflictType;
    private Boolean blocking;
    private TimetableConflictStatus status;
    private String message;
    private Long entryId;
    private Long relatedEntryId;
    private Long sectionId;
    private String sectionName;
    private Long teacherAllocationId;
    private Long resourceId;
    private DayOfWeek dayOfWeek;
    private Long periodId;
    private LocalDateTime resolvedAt;
    private Long resolvedByUserId;
}
