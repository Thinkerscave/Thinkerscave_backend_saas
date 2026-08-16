package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.AcademicStage;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class AcademicClassResponse {
    private Long classId;
    private Long academicYearId;
    private String academicYearName;
    private AcademicYearStatus academicYearStatus;
    private boolean yearReadOnly;

    private String name;
    private String code;
    private AcademicStage stage;
    private Integer displayOrder;
    private Boolean active;

    private long sectionCount;
    private long sectionsActive;
    private long studentCount;

    /**
     * @deprecated Class Teacher is a Section-level relationship. These fields are
     * always null and kept only for JSON compatibility with older clients.
     */
    @Deprecated
    private String classTeacherName;
    /** @deprecated See {@link #classTeacherName}. */
    @Deprecated
    private Long classTeacherStaffId;

    private List<AcademicSectionResponse> sections;

    private String createdBy;
    private LocalDateTime createdOn;
    private String updatedBy;
    private LocalDateTime updatedOn;

    /** Compatibility aliases used by legacy FE callers. */
    public String getClassName() {
        return name;
    }

    public String getClassCode() {
        return code;
    }

    public String getAcademicStage() {
        return stage == null ? null : stage.name();
    }

    public String getYearCode() {
        return academicYearName;
    }
}
