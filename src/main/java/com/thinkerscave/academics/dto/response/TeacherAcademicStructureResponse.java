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
    private List<AcademicGroupNode> academicGroups;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AcademicGroupNode {
        private Long academicGroupId;
        private String academicGroupName;
        private String academicGroupCode;
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
