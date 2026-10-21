package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class TimetableSlotItem {
    private Long timetableEntryId;
    private Long academicYearId;
    private Long classId;
    private Long sectionId;
    private Long periodId;
    private Integer periodNumber;
    private String periodName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String subjectName;
    private String teacherName;
    private String className;
    private String roomLabel;
    private String attendanceMode;
    private boolean attendanceAllowed;
    private boolean substitute;
    private String attendanceStatus;
    private Integer expectedStudents;
    private Integer markedStudents;
    private boolean canViewAttendance;
    private boolean canMarkAttendance;
}
