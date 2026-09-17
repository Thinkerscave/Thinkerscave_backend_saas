package com.thinkerscave.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReleaseRequest(
        @NotBlank @Size(max = 50) String releaseVersion,
        @NotBlank @Size(max = 50) String applicationVersion,
        @NotBlank @Size(max = 50) String targetDatabaseVersion,
        @NotBlank @Size(max = 50) String targetCatalogVersion,
        @Size(max = 4000) String releaseNotes
) {
}
