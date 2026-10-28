package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.AcademicYearStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class SubjectMappingBoardResponse {
    private Long academicGroupId;
    private String academicGroupName;
    private String academicGroupCode;
    private Long academicYearId;
    private String academicYearName;
    private AcademicYearStatus academicYearStatus;
    private boolean yearReadOnly;
    private List<SubjectMappingRowResponse> mappings;
    private long mappedCount;
    private long compulsoryCount;
    private long optionalCount;
    private long teacherPendingCount;

}
