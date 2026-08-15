package com.thinkerscave.academics.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAcademicStructureResponse {

    private Long academicYearId;
    private String academicYearName;
    private List<ClassNode> classes;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ClassNode {
        private Long classId;
        private String className;
        private String classCode;
        private List<SectionNode> sections;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SectionNode {
        private Long sectionId;
        private String sectionName;
        private String sectionCode;
        private List<SubjectNode> subjects;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SubjectNode {
        private Long subjectId;
        private String subjectName;
        private String subjectCode;
        private short weeklyPeriods;
    }
}
