package com.thinkerscave.academics.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentMyAcademicsResponse {

    private String studentName;
    private String admissionNumber;
    private Long classId;
    private String className;
    private Long sectionId;
    private String sectionName;
    private String rollNumber;
    private Long academicYearId;
    private String academicYearName;
    private List<SubjectInfo> subjects;
    private ClassTeacherInfo classTeacher;
    private boolean publishedTimetableExists;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SubjectInfo {
        private Long subjectId;
        private String subjectName;
        private String subjectCode;
        private short weeklyPeriods;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ClassTeacherInfo {
        private Long staffId;
        private String staffName;
    }
}
