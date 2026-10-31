package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.AcademicYearStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/** State-aware payload for the single Timetable workspace. */
@Getter
@Setter
@Builder
public class TimetableWorkspaceResponse {

    private Long academicYearId;
    private String academicYearName;
    private AcademicYearStatus academicYearStatus;
    private boolean yearActive;
    private boolean yearReadOnly;
    /** NOT_GENERATED | DRAFT | PUBLISHED */
    private String state;
    private String overallStatus;
    private boolean canGenerate;
    private String generateBlockedReason;
    private TimetableReadinessResponse readiness;
    private TimetableFiltersResponse filters;
    private List<SetupItem> setup;
    private List<Issue> issues;
    private int blockingIssueCount;
    private int warningIssueCount;
    private int unscheduledLessonCount;
    private TimetableVersionResponse currentVersion;
    private TimetableVersionResponse latestVersion;
    private TimetableVersionResponse workingVersion;
    private List<TimetableVersionResponse> versions;
    private LocalDate referenceDate;

    @Getter
    @Setter
    @Builder
    public static class SetupItem {
        private String key;
        private String label;
        private String status;
        private String summary;
        private int completed;
        private int total;
        private String actionLabel;
        private String actionRoute;
    }

    @Getter
    @Setter
    @Builder
    public static class Issue {
        private String code;
        private String title;
        private String severity;
        private String type;
        private String academicGroupName;
        private String sectionName;
        private String subjectName;
        private String teacherName;
        private String timeSlot;
        private String message;
        private String actionLabel;
        private String actionRoute;
    }
}
