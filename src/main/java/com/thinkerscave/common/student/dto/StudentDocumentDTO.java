package com.thinkerscave.common.student.dto;

import lombok.Data;

@Data
public class StudentDocumentDTO {
    private Long documentId;
    private String documentName;
    private String documentType;
}
