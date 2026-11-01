package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.DayOfWeek;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class FixedActivityRequest {

    /** Active academic, practical or activity subject from the reusable catalog. */
    @NotNull(message = "Select a subject")
    private Long subjectId;

    @Size(max = 500)
    private String description;

    /** Ignored: new records are always active and status changes use activate/deactivate. */
    private Boolean active = true;

    @NotNull(message = "Select at least one applicable day")
    private Set<DayOfWeek> days = new LinkedHashSet<>();

    @NotNull(message = "Select at least one academic group or section")
    private List<Unit> groups = new ArrayList<>();

    /**
     * Logical period label (for example "Period 3"). The server resolves the
     * configured period for every group/section/day combination.
     */
    @NotBlank(message = "Select a configured period")
    @Size(max = 50)
    private String periodName;

    /**
     * Retained for older clients but ignored. The teacher is always resolved from the
     * existing Teacher Allocation records and cannot be reassigned from Fixed Activity.
     */
    @Deprecated
    private Long staffId;

    @Getter
    @Setter
    public static class Unit {
        private Long academicGroupId;
        /** Null means the whole group, or all active sections when the group has sections. */
        private Long sectionId;
    }
}
