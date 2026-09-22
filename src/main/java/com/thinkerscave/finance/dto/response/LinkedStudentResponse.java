package com.thinkerscave.finance.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LinkedStudentResponse {
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private String relationship;
}
