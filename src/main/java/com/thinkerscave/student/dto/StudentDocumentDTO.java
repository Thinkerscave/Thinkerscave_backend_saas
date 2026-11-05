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
    private String fileName;
    private String fileExtension;
    private String mimeType;
    private Long fileSize;
    private String uploadedOn;
    private Long sourceApplicationDocumentId;
    private Long replacedDocumentId;
    private Boolean active;
    /** Human label (e.g. custom admission document name). Prefer this in UI. */
    private String displayLabel;
    private DocumentCheckStatus status;
    private String remarks;
}
