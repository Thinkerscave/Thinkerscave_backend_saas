package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AcademicYearTransitionRequest {

    @NotNull
    private Long targetAcademicYearId;

    private boolean copyClasses = true;
    private boolean copySections = true;
    private boolean copySubjects = true;
    private boolean copyMappings = true;
    private boolean copyAllocations;
}
