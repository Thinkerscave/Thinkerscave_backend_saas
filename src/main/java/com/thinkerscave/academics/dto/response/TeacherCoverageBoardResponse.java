package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.enums.SubjectMappingScope;
import com.thinkerscave.academics.enums.SubjectRequirement;
import com.thinkerscave.academics.enums.SubjectType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class TeacherCoverageBoardResponse {
    private Long academicYearId;
    private String academicYearName;
    private AcademicYearStatus academicYearStatus;
    private boolean yearReadOnly;
    private Long academicGroupId;
    private String academicGroupName;
    private String academicGroupCode;
    private boolean hasSections;
    private List<SectionOption> sections;
    private Summary summary;
    private List<Row> rows;

    @Getter
    @Setter
    @Builder
    public static class SectionOption {
        private Long sectionId;
        private String name;
        private String code;
    }

    @Getter
    @Setter
    @Builder
    public static class Summary {
        private long subjectsRequiringTeachers;
        private long fullyAllocated;
        /** Number of teachers whose weekly load exceeds their configured capacity. */
        private long workloadWarnings;
        /** Null until a generated timetable exists to check against. */
        private Long timetableConflicts;
        /** AVAILABLE_AFTER_TIMETABLE_GENERATION until a timetable exists, then CHECKED. */
        private String timetableConflictStatus;
    }

    @Getter
    @Setter
    @Builder
    public static class TeacherRef {
        private Long staffId;
        private String name;
        private String staffCode;
    }

    @Getter
    @Setter
    @Builder
    public static class SectionAssignment {
        private Long sectionId;
        private String sectionName;
        private TeacherRef teacher;
        /** INHERITED (from the group default), OVERRIDE (section-specific), or NONE. */
        private String source;
    }

    @Getter
    @Setter
    @Builder
    public static class Row {
        private Long mappingId;
        private Long subjectId;
        private String subjectName;
        private String subjectCode;
        private SubjectType subjectType;
        private Short weeklyPeriods;
        private SubjectRequirement requirement;
        private SubjectMappingScope scope;
        private int sectionsTotal;
        private int sectionsAssigned;
        private TeacherRef groupTeacher;
        /** Single teacher shared by every assigned section, or null when none or several teachers are used. */
        private TeacherRef assignedTeacher;
        private boolean multipleTeachers;
        /** ASSIGNED, PARTIAL, or UNASSIGNED. */
        private String status;
        /** AVAILABLE, LIMITED, AT_CAPACITY, or EXCEEDS_LIMIT for the assigned teacher. */
        private String workloadStatus;
        private List<SectionAssignment> sections;
    }
}
