package com.thinkerscave.common.course.dto;

import com.thinkerscave.common.course.enums.SyllabusStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for sending Syllabus details to the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyllabusResponseDTO {
    private Long syllabusId;
    private String syllabusCode;
    private String title;
    private String description;
    private String version;
    private SyllabusStatus status;
    private String subjectName;
    private String academicYear;
    private List<ChapterDTO> chapters;
    private LocalDate approvedDate;
    private LocalDate publishedDate;
}
