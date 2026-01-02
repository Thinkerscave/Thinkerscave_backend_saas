package com.thinkerscave.common.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Chapter within a Syllabus.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterDTO {
    private Integer chapterNumber;
    private String chapterName;
    private String description;
    private String learningObjectives;
    private List<TopicDTO> topics;
}
