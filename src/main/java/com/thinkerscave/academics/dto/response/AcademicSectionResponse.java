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
    private Long classId;
    private String className;
    private String classCode;

    private String name;
    private String code;
    private Integer capacity;
    private Integer displayOrder;
    private Long defaultResourceId;
    private Boolean active;

    private long studentCount;
    private String classTeacherName;
    private Long classTeacherStaffId;

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
