package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.AcademicStage;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class AcademicGroupMappingBoardResponse {
    private Long academicGroupId;
    private String academicGroupName;
    private String academicGroupCode;
    private AcademicStage stage;
    private List<String> sectionNames;
    private List<AcademicGroupSubjectMappingResponse> mappings;
    private long includedCount;
    private long missingTeacherCount;
}
