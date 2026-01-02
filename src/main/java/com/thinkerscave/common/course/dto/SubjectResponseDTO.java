package com.thinkerscave.common.course.dto;

import com.thinkerscave.common.course.enums.SubjectCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sending Subject details to the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectResponseDTO {
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private String description;
    private SubjectCategory category;
    private Integer credits;
    private Integer theoryHours;
    private Integer labHours;
    private Integer practicalHours;
    private Boolean isActive;
}
