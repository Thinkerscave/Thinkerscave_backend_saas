package com.thinkerscave.common.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSubjectMappingDTO {
    private Long mappingId;
    private Long courseId;
    private Long subjectId;
    private String subjectName; // For response convenience
    private String subjectCode; // For response to identify subject
    private Integer semester;
    private Boolean isMandatory;
    private Integer displayOrder;
}
