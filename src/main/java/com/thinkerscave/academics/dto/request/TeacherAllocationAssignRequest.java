package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.TeacherAllocationTeacherRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeacherAllocationAssignRequest {

    @NotNull(message = "Section ID is mandatory")
    private Long sectionId;

    @NotNull(message = "Academic Group subject mapping ID is mandatory")
    private Long academicGroupSubjectMappingId;

    @NotNull(message = "Staff ID is mandatory")
    private Long staffId;

    private TeacherAllocationTeacherRole role = TeacherAllocationTeacherRole.PRIMARY;
}
