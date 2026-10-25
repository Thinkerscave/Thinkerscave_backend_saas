package com.thinkerscave.academics.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherMyAcademicGroupsResponse {

    private Summary summary;
    private List<AcademicGrouplard> academicGroups;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Summary {
        private int academicGroupCount;
        private int sectionCount;
        private int subjectCount;
        private int weeklyPeriods;
        private int studentCount;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AcademicGrouplard {
        private Long academicGroupId;
        private String academicGroupName;
        private String academicGroupCode;
        private Long sectionId;
        private String sectionName;
        private Integer studentCount;
        private String roomName;
        private List<SubjectSlot> subjects;
        private boolean sectionTeacher;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SubjectSlot {
        private Long subjectId;
        private String subjectName;
        private short weeklyPeriods;
        private Long allocationId;
    }
}
