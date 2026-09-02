package com.thinkerscave.admission.dto.request;

import com.thinkerscave.admission.enums.ApplicationStatus;
import lombok.Data;

import java.util.List;

@Data
public class ApplicationSearchRequest {

    private String keyword;
    private ApplicationStatus status;
    private List<ApplicationStatus> statuses;
    private String applyingForClass;
}