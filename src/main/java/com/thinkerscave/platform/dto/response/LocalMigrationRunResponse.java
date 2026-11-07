package com.thinkerscave.platform.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record LocalMigrationRunResponse(
        boolean executed,
        String outcome,
        String message,
        String beforeVersion,
        String afterVersion,
        int migrationsExecuted,
        List<ScriptStateResponse> appliedScripts,
        LocalDateTime completedAt
) {
}
