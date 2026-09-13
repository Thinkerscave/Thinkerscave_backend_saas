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

    /**
     * MY = applications owned by the current counselor/user.
     * ALL = organization-wide (approvers / elevated roles only).
     * Server may force MY when the caller lacks APPROVE.
     */
    private String scope;

    /** Set by service for MY scope — matches Auditable.createdBy (username). */
    private String createdBy;

    /** Set by service for MY scope — matches Inquiry.assignedCounselorId. */
    private Long assignedCounselorId;
}
