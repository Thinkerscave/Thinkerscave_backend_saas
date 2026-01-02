package com.thinkerscave.common.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Topic within a Chapter.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicDTO {
    private Integer topicNumber;
    private String topicName;
    private String description;
    private Integer estimatedHours;
}
