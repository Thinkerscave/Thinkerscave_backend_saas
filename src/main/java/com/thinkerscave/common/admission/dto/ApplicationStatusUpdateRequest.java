package com.thinkerscave.common.admission.dto;

import com.thinkerscave.common.admission.domain.ApplicationStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ApplicationStatusUpdateRequest {
    @NotNull(message = "Status is required")
    private ApplicationStatus status;
    
    @NotEmpty(message = "At least one application ID is required")
    private List<String> applicationIds;
}
