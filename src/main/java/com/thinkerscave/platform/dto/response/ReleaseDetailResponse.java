package com.thinkerscave.platform.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ReleaseDetailResponse(
        ReleaseOverviewResponse release,
        ReleaseValidationResponse validation,
        List<ExecutionJobResponse> tenantExecutions,
        List<AuditEventResponse> timeline,
        LocalDateTime checkedAt
) {
}
