package com.thinkerscave.student.dto;

import com.thinkerscave.admission.enums.DocumentCheckStatus;

/**
 * Already-stored file metadata to attach to a student (e.g. after admissions enroll).
 */
public record ImportedStudentDocument(
        String documentType,
        String documentName,
        String storedPath,
        DocumentCheckStatus status,
        Long sourceApplicationDocumentId,
        String remarks
) {
    public ImportedStudentDocument(
            String documentType,
            String documentName,
            String storedPath,
            DocumentCheckStatus status,
            Long sourceApplicationDocumentId) {
        this(documentType, documentName, storedPath, status, sourceApplicationDocumentId, null);
    }
}
