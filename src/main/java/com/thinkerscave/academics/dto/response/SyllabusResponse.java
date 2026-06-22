package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class SyllabusResponse {
    private Long syllabusId;
    private Long academicYearId;
    private Long classId;
    private String className;
    private Long subjectId;
    private String subjectName;
    private String title;
    private String versionNo;
    private Boolean published;
    private Boolean active;
    private List<UnitResponse> units;
}
