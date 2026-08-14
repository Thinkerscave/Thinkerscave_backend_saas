package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.TimetableGenerationStatus;
import com.thinkerscave.academics.timetable.engine.GenerationPhase;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TimetableGenerationProgressResponse {

    private Long generationId;
    private Long timetableVersionId;
    private Integer versionNumber;
    private TimetableGenerationStatus status;
    private GenerationPhase phase;
    private String phaseLabel;
    private int progressPercent;
    private TimetableGenerateResultResponse result;
    private String message;
    private String algorithmVersion;
}
