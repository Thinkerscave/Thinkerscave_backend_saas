package com.thinkerscave.academics.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TimetableGenerationStartRequest {

    private Long seed;

    /** Empty/null means all eligible groups. */
    private List<Long> academicGroupIds = new ArrayList<>();

    /** Preserve lessons that are locked in the existing draft. */
    private Boolean preserveLocked = true;

    /** Explicit confirmation is required when a draft will be replaced. */
    private Boolean replaceExistingDraft = false;

    /** User-selected reference week for pattern effective dates. */
    private java.time.LocalDate referenceDate;
}
