package com.thinkerscave.finance.dto.request;

import com.thinkerscave.finance.enums.FeeHeadCategory;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeeHeadRequest {
    @NotBlank @Size(max = 100)
    private String name;
    @NotNull
    private FeeHeadCategory category;
    @Size(max = 500)
    private String description;
    private FeeMasterStatus status;
}
