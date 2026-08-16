package com.thinkerscave.academics.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherMyClassesResponse {

    private Summary summary;
    private List<ClassCard> classes;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Summary {
        private int classCount;
        private int sectionCount;
        private int subjectCount;
        private int weeklyPeriods;
        private int studentCount;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ClassCard {
        private Long classId;
        private String className;
        private String classCode;
        private Long sectionId;
        private String sectionName;
        private Integer studentCount;
        private String roomName;
        private List<SubjectSlot> subjects;
        private boolean classTeacher;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SubjectSlot {
        private Long subjectId;
        private String subjectName;
        private short weeklyPeriods;
        private Long allocationId;
    }
}
