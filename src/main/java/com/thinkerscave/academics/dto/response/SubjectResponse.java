package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.enums.SubjectCategory;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class SubjectResponse {
    private Long subjectId;
    private Long academicYearId;
    private String academicYearName;
    private AcademicYearStatus academicYearStatus;
    private boolean yearReadOnly;

    private String name;
    private String code;
    private SubjectCategory category;
    private Short defaultWeeklyPeriods;
    private SubjectTimetablePreference timetablePreference;
    private String description;
    private Boolean active;

    private long mappedClassCount;
    private long teacherAllocationCount;

    private List<ClassSubjectMappingResponse> mappings;

    private String createdBy;
    private LocalDateTime createdOn;
    private String updatedBy;
    private LocalDateTime updatedOn;

    public String getSubjectName() {
        return name;
    }

    public String getSubjectCode() {
        return code;
    }

    public String getSubjectType() {
        return category == null ? null : category.name();
    }
}
