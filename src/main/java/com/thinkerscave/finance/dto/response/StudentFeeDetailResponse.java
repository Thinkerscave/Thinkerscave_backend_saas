package com.thinkerscave.finance.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentFeeDetailResponse {
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private String className;
    private String sectionName;
    private Long academicYearId;
    private String academicYearName;
    private StudentFeeKpiResponse kpis;
    private boolean canCollectFee;
}
