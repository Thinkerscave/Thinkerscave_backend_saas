package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SubjectResponse {
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private String subjectType;
    private Boolean active;
    private String remarks;
}
