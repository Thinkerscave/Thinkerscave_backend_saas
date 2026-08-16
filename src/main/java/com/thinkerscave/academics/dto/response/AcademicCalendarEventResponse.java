package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.CalendarAudienceType;
import com.thinkerscave.academics.enums.CalendarEventStatus;
import com.thinkerscave.academics.enums.CalendarEventType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
public class AcademicCalendarEventResponse {

    private Long eventId;
    private Long academicYearId;
    private String academicYearName;
    private boolean yearReadOnly;

    private String title;
    private String description;
    private CalendarEventType eventType;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean allDay;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private CalendarAudienceType audienceType;
    private CalendarEventStatus status;

    private List<CalendarClassRef> classes;
    private List<CalendarSectionRef> sections;

    private String publishedBy;
    private LocalDateTime publishedOn;

    private String createdBy;
    private LocalDateTime createdOn;
    private String updatedBy;
    private LocalDateTime updatedOn;

    @Getter
    @Setter
    @Builder
    public static class CalendarClassRef {
        private Long classId;
        private String name;
        private String code;
    }

    @Getter
    @Setter
    @Builder
    public static class CalendarSectionRef {
        private Long sectionId;
        private String name;
        private String code;
        private String className;
    }
}
