package com.thinkerscave.platform.dto.response;

import java.time.LocalDateTime;

public record ScriptStateResponse(
        String version,
        String description,
        String script,
        String state,
        Integer checksum,
        LocalDateTime installedOn,
        Integer executionTimeMs
) {
}
