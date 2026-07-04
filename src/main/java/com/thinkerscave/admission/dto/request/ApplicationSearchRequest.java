package com.thinkerscave.admission.dto.request;

import com.thinkerscave.admission.enums.ApplicationStatus;
import lombok.Data;

@Data
public class ApplicationSearchRequest {

    private String keyword;
    private ApplicationStatus status;
    private String applyingForClass;
}