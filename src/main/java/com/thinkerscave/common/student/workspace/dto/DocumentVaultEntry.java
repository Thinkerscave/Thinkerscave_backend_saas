package com.thinkerscave.common.student.workspace.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DocumentVaultEntry {
    private Long documentId;
    private Long studentId;
    private String studentName;
    private String documentType;
    private String fileName;
    private String fileUrl;
    private String status;          // "VERIFIED" | "PENDING" | "MISSING"
    private String category;        // "PERSONAL" | "ACADEMIC" | "MEDICAL" | "OTHER"
    private LocalDate uploadedOn;
    private String verifiedBy;
    private LocalDate verifiedOn;
    private LocalDate expiresOn;
    private String remarks;
}
