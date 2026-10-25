package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.SubjectType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SubjectResponse {
    private Long subjectId;
    private String name;
    private String code;
    private SubjectType subjectType;
    private String description;
    private Boolean active;

    /** Active curriculum mappings across all academic years. */
    private long mappedAcademicGroupCount;
    /** Sections covered by active mappings. */
    private long mappedSectionCount;

    private String createdBy;
    private LocalDateTime createdOn;
    private String updatedBy;
    private LocalDateTime updatedOn;
}
