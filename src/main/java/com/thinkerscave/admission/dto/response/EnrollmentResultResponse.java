package com.thinkerscave.admission.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnrollmentResultResponse {

    private Long applicationId;
    private String applicationNumber;
    private Long studentId;
    private String studentCode;
    private String admissionNumber;
    private String studentName;
    private Long academicYearId;
    private Long classId;
    private Long sectionId;
}
