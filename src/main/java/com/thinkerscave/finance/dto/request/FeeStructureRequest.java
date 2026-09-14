package com.thinkerscave.finance.dto.request;

import com.thinkerscave.finance.enums.FeeMasterStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class FeeStructureRequest {
    @NotBlank @Size(max = 150)
    private String name;
    @NotNull
    private Long academicYearId;
    @NotNull
    private Long classId;
    @NotNull @Min(1) @Max(28)
    private Short dueDay;
    private FeeMasterStatus status;
    @Valid
    private List<FeeStructureItemRequest> items;
}
