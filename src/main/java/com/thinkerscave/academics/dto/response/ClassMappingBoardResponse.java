package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.AcademicStage;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ClassMappingBoardResponse {
    private Long classId;
    private String className;
    private String classCode;
    private AcademicStage stage;
    private List<String> sectionNames;
    private List<ClassSubjectMappingResponse> mappings;
    private long includedCount;
    private long missingTeacherCount;
}
