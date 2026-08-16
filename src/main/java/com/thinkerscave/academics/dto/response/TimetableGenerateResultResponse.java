package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.TimetableGenerationStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TimetableGenerateResultResponse {

    private Long timetableVersionId;
    private Integer versionNumber;
    private TimetableGenerationStatus generationStatus;
    private long totalEntries;
    private long totalConflicts;
    private long openBlockingConflicts;
    private String message;
    private String algorithmVersion;
    private ResultKind resultKind;

    public enum ResultKind {
        SUCCESS,
        SUCCESS_WITH_WARNINGS,
        BLOCKED,
        FAILED
    }
}
