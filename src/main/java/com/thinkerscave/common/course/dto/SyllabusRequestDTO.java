package com.thinkerscave.common.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for creating or updating a Syllabus.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyllabusRequestDTO {
    private String syllabusCode;
    private String title;
    private String description;
    private String version;
    private Long subjectId;
    private Long academicYearId;
    private Long previousVersionId;
    private List<ChapterDTO> chapters;
}
