package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.TimetableGenerationStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TimetableGenerationAcceptedResponse {

    private Long generationId;
    private Long timetableVersionId;
    private Integer versionNumber;
    private TimetableGenerationStatus status;
    private String algorithmVersion;
}
