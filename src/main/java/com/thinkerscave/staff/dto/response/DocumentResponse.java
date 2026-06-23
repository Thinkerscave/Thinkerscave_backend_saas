package com.thinkerscave.staff.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DocumentResponse {

    private Long documentId;
    private String ownerType;
    private Long ownerId;
    private String documentType;
    private String documentName;
    private String fileName;
    private String filePath;
    private String fileExtension;
    private Long fileSize;
    private String mimeType;
    private Boolean active;
}
