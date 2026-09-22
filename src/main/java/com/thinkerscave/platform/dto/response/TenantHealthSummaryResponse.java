package com.thinkerscave.platform.dto.response;

import java.time.LocalDateTime;

public record TenantHealthSummaryResponse(
        long totalTenants,
        long healthy,
        long warning,
        long maintenance,
        long critical,
        LocalDateTime checkedAt
) {}
