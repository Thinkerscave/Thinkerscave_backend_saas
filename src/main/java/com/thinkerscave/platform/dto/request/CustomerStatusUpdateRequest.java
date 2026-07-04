package com.thinkerscave.platform.dto.request;

import com.thinkerscave.platform.enums.CustomerStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerStatusUpdateRequest {

    @NotNull
    private CustomerStatus status;
}
