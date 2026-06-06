package com.thinkerscave.common.admission.dto;

import com.thinkerscave.common.admission.domain.ApplicationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdmissionSearchRequest {
    private String keyword;
    private String admissionId;
    private String studentName;
    private String mobileNumber;
    private String parentName;
    private ApplicationStatus status;
    private String classApplied;
}
