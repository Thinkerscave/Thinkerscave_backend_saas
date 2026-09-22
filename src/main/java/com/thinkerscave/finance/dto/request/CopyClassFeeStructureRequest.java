package com.thinkerscave.finance.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Copy an existing class fee structure to other classes in the same academic year.
 */
@Data
public class CopyClassFeeStructureRequest {
    @NotEmpty
    private List<Long> targetClassIds;

    @NotNull
    private OnConflict onConflict = OnConflict.SKIP;

    public enum OnConflict {
        /** Leave already-configured target classes unchanged. */
        SKIP,
        /** Deactivate existing ACTIVE structure and create a new copy. */
        REPLACE
    }
}
