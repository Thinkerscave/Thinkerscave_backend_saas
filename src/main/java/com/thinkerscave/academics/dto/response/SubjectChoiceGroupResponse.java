package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.SubjectType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SubjectChoiceGroupResponse {
    private Long subjectChoiceGroupId;
    private String name;
    private SubjectType subjectType;
    private boolean active;
    private List<SubjectChoiceGroupSubjectResponse> subjects;
    private LocalDateTime createdOn;
}
