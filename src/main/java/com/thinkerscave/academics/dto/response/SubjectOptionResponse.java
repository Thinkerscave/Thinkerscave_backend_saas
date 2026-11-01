package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.SubjectType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubjectOptionResponse {
    private Long subjectId;
    private String name;
    private String code;
    private SubjectType subjectType;
}
