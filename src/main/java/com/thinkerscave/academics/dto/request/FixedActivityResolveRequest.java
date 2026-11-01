package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.DayOfWeek;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Input for resolving actual schedule patterns and periods for a fixed activity. */
@Getter
@Setter
public class FixedActivityResolveRequest {

    private Long subjectId;

    private List<Unit> groups;

    /** Empty means resolve every weekday so the UI can show supported days. */
    private Set<DayOfWeek> days = new LinkedHashSet<>();

    private String periodName;

    @Getter
    @Setter
    public static class Unit {
        private Long academicGroupId;
        private Long sectionId;
    }
}
