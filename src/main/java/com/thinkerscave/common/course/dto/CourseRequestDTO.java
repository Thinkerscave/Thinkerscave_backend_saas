package com.thinkerscave.common.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating a Course.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequestDTO {
    private String courseCode;
    private String courseName;
    private String description;
    private String category;
    private Integer durationYears;
    private Integer totalSemesters;
    private String eligibilityCriteria;
    private Double fees;
    private Long organizationId;
}
