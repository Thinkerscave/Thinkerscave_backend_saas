package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ClassTeacherAssignmentRequest {

    @NotNull(message = "Section ID is mandatory")
    private Long sectionId;

    @NotNull(message = "Teacher (staff) ID is mandatory")
    private Long staffId;

    private LocalDate effectiveFrom;
}
