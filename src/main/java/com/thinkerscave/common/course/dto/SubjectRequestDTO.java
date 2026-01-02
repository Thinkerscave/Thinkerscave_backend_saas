package com.thinkerscave.common.course.dto;

import com.thinkerscave.common.course.enums.SubjectCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating a Subject.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectRequestDTO {
    private String subjectCode;
    private String subjectName;
    private String description;
    private SubjectCategory category;
    private Integer credits;
    private Integer theoryHours;
    private Integer labHours;
    private Integer practicalHours;
    private Long organizationId;
}
