package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AcademicSectionResponse {
    private Long sectionId;
    private Long academicGroupId;
    private String academicGroupName;
    private String academicGroupCode;

    private String name;
    private String code;
    private Integer capacity;
    private Integer displayOrder;
    private Long defaultResourceId;
    private Boolean active;

    private long studentCount;
    private String sectionTeacherName;
    private Long sectionTeacherStaffId;

    private String createdBy;
    private LocalDateTime createdOn;
    private String updatedBy;
    private LocalDateTime updatedOn;

    public String getSectionName() {
        return name;
    }

    public String getSectionCode() {
        return code;
    }
}
