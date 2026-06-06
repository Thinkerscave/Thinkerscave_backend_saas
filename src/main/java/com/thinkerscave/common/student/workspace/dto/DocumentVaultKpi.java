package com.thinkerscave.common.student.workspace.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentVaultKpi {
    private long totalDocuments;
    private long verifiedDocuments;
    private long pendingVerification;
    private long missingDocuments;
}
