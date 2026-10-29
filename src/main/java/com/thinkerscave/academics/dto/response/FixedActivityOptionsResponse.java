package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Searchable values for the fixed activity form. */
@Getter
@Setter
@Builder
public class FixedActivityOptionsResponse {

    private Long academicYearId;
    private List<SubjectOption> subjects;
    private List<GroupOption> groups;

    @Getter
    @Setter
    @Builder
    public static class SubjectOption {
        private Long subjectId;
        private String name;
        private String code;
        /** ACADEMIC_SUBJECT, PRACTICAL_LAB or ACTIVITY. */
        private String subjectType;
    }

    @Getter
    @Setter
    @Builder
    public static class GroupOption {
        private Long academicGroupId;
        private String name;
        private String code;
        private List<SectionOption> sections;
    }

    @Getter
    @Setter
    @Builder
    public static class SectionOption {
        private Long sectionId;
        private String name;
        private String code;
    }
}
