package com.thinkerscave.common.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableSlotDTO {
    private Long slotId;
    private Long organizationId;
    private Long academicYearId;
    private Long classId;
    private String className;
    private Long sectionId;
    private String sectionName;
    private Long subjectId;
    private String subjectName;
    private Long teacherId;
    private String teacherName;
    private DayOfWeek dayOfWeek;
    private Integer periodNumber;
    private LocalTime startTime;
    private LocalTime endTime;
    private String roomName;
    private Boolean isActive;
}