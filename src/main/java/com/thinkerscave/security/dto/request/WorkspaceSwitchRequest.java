package com.thinkerscave.security.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkspaceSwitchRequest {

    @NotNull
    private Long organizationId;
}
