package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.SubjectType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AvailableSubjectResponse {
    private Long subjectId;
    private String subjectName;
    private String subjectCode;
    private SubjectType subjectType;
    private boolean subjectActive;
    private boolean mapped;
    private Long mappingId;
}
