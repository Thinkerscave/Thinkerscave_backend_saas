package com.thinkerscave.admission.dto.response;

import org.springframework.core.io.Resource;

/**
 * Download payload with original filename and MIME type for inline preview.
 */
public record ApplicationDocumentFile(
        Resource resource,
        String originalName,
        String contentType
) {
}
