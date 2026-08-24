package com.thinkerscave.admission.dto.response;

import com.thinkerscave.admission.enums.DocumentCheckStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApplicationDocumentResponse {

    private Long documentId;
    private Long applicationId;
    private String documentType;
    private String originalName;
    private DocumentCheckStatus status;
    private String remarks;
    private LocalDateTime createdOn;
    private String createdBy;
}
