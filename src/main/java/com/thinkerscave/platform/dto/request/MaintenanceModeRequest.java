package com.thinkerscave.platform.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MaintenanceModeRequest(
        @NotNull Boolean enabled,
        @Size(max = 500) String reason
) {}
