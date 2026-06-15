package com.thinkerscave.student.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentDocumentDTO {
    private Long documentId;
    private String documentName;
    private String documentType;
}
