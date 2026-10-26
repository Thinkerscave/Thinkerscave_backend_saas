package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.TimetableGenerationStatus;
import com.thinkerscave.academics.enums.TimetableStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class TimetableVersionResponse {

    private Long timetableVersionId;
    private Long academicYearId;
    private Long timetableConfigurationId;
    private Integer versionNumber;
    private TimetableGenerationStatus generationStatus;
    private TimetableStatus status;
    private LocalDateTime generatedAt;
    private LocalDateTime approvedAt;
    private Long approvedByUserId;
    private LocalDateTime publishedAt;
    private Long publishedByUserId;
    private LocalDateTime supersededAt;
    private long totalEntries;
    private long totalConflicts;
    private long openBlockingConflicts;
    private long unplacedLessonCount;
    private boolean editable;
    private List<Event> events;

    @Getter
    @Setter
    @Builder
    public static class Event {
        private Long eventId;
        private String eventType;
        private String fromStatus;
        private String toStatus;
        private String note;
        private LocalDateTime at;
        private Long actorUserId;
    }
}
