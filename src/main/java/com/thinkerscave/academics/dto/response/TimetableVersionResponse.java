package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.TimetableGenerationStatus;
import com.thinkerscave.academics.enums.TimetableStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
}
