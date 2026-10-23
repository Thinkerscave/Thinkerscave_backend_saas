package com.thinkerscave.academics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class TimetableReadinessResponse {

    private boolean ready;
    private ReadinessSummary summary;
    private List<ReadinessCheck> checks;
    private List<ReadinessCheck> blockingIssues;
    private List<ReadinessCheck> warnings;
    private Long configurationId;
    private String configurationName;

    @Getter
    @Setter
    @Builder
    public static class ReadinessCheck {
        private String code;
        private String title;
        private ReadinessStatus status;
        private ReadinessSeverity severity;
        private String message;
        private String reference;
        /** Specific items (class/section/subject, teacher, period) that need attention. */
        private List<String> details;
        private String actionLabel;
        /** Frontend route or in-page tab key ({@code tab:configuration}) that fixes the issue. */
        private String actionRoute;
    }

    @Getter
    @AllArgsConstructor
    public static class ReadinessSummary {
        private int sections;
        private int subjects;
        private int requirements;
        private int teachers;
        private int resources;
        private int weeklySlots;
        private int unscheduledSubjects;
    }

    public enum ReadinessStatus {
        PASSED, FAILED, WARNING
    }

    public enum ReadinessSeverity {
        BLOCKING, WARNING
    }
}
