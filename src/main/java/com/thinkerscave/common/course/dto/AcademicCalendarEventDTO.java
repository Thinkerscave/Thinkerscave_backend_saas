package com.thinkerscave.common.course.dto;

import com.thinkerscave.common.course.enums.AcademicEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicCalendarEventDTO {
    private Long eventId;
    private Long organizationId;
    private Long academicYearId;
    private String title;
    private AcademicEventType eventType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean allDay;
    private Boolean isActive;
    private String description;
}