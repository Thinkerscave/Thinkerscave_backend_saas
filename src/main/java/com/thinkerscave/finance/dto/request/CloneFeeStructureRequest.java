package com.thinkerscave.finance.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CloneFeeStructureRequest {
    @NotNull
    private Long targetAcademicYearId;
    @NotEmpty @Valid
    private List<CloneTarget> targets;
    @NotNull
    private OnConflict onConflict = OnConflict.CANCEL_CLASS;

    public enum OnConflict { CANCEL_CLASS, REPLACE_DEACTIVATE }

    @Data
    public static class CloneTarget {
        @NotNull
        private Long classId;
        private String name;
        private Short dueDay;
        @Valid
        private List<FeeStructureItemRequest> items;
    }
}
