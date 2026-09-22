package com.thinkerscave.shared.document.dto;

import com.thinkerscave.shared.document.enums.DocumentType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StoreDocumentCommand {
    DocumentType documentType;
    String ownerType;
    Long ownerId;
    String periodKey;
    String idempotencyKey;
    byte[] content;
    String contentType;
    String fileName;
}
