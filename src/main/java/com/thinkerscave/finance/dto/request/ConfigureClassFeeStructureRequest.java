package com.thinkerscave.finance.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Configure / update fee structure for a known class + academic year.
 * Structure name is generated server-side — never collected from the UI.
 */
@Data
public class ConfigureClassFeeStructureRequest {
    @NotNull
    private Long academicYearId;
    @NotNull
    private Long classId;
    /** Optional — defaults from Finance settings / existing structure when omitted. */
    @Min(1)
    @Max(28)
    private Short dueDay;
    @Valid
    private List<FeeStructureItemRequest> items;
}
