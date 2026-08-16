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

    @Getter
    @Setter
    @Builder
    public static class ReadinessCheck {
        private String code;
        private ReadinessStatus status;
        private ReadinessSeverity severity;
        private String message;
        private String reference;
    }

    @Getter
    @AllArgsConstructor
    public static class ReadinessSummary {
        private int sections;
        private int subjects;
        private int requirements;
        private int teachers;
        private int resources;
    }

    public enum ReadinessStatus {
        PASSED, FAILED, WARNING
    }

    public enum ReadinessSeverity {
        BLOCKING, WARNING
    }
}
