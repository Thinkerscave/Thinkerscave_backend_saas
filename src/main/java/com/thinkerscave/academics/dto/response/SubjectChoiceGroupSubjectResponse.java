package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubjectChoiceGroupSubjectResponse {
    private Long subjectId;
    private String name;
    private String code;
    private boolean active;
}
