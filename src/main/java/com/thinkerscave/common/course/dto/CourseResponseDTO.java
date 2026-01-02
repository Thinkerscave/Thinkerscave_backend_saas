package com.thinkerscave.common.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sending Course details to the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponseDTO {
    private Long courseId;
    private String courseCode;
    private String courseName;
    private String description;
    private String category;
    private Integer durationYears;
    private Integer totalSemesters;
    private String eligibilityCriteria;
    private Double fees;
    private Boolean isActive;
}
