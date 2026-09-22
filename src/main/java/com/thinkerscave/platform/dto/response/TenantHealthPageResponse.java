package com.thinkerscave.platform.dto.response;

import org.springframework.data.domain.Page;

public record TenantHealthPageResponse(
        TenantHealthSummaryResponse summary,
        Page<TenantHealthResponse> tenants
) {}
