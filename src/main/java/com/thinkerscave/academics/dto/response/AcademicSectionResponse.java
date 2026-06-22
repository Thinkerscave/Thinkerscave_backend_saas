package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AcademicSectionResponse {
    private Long sectionId;
    private Long classId;
    private String className;
    private String sectionName;
    private Integer capacity;
    private Boolean active;
    private String remarks;
    private LocalDateTime createdOn;
}
