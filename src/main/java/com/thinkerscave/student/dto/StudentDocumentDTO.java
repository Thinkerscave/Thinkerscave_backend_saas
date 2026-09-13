package com.thinkerscave.student.dto;

import com.thinkerscave.admission.enums.DocumentCheckStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentDocumentDTO {
    private Long documentId;
    private String documentName;
    private String documentType;
    /** Human label (e.g. custom admission document name). Prefer this in UI. */
    private String displayLabel;
    private DocumentCheckStatus status;
    private String remarks;
}
