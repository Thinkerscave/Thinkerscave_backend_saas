package com.thinkerscave.common.student.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DocumentVaultRequest {
    @NotNull
    private Long studentId;

    @NotBlank
    private String category;

    @NotBlank
    private String documentType;

    @NotBlank
    private String fileName;

    private String fileUrl;
    private Long fileSize;
    private String status;
    private LocalDate expiresOn;
    private String remarks;
}
