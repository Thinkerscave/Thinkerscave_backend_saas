package com.thinkerscave.student.dto;

import com.thinkerscave.student.enums.StudentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentStatusUpdateRequest {

    @NotNull
    private StudentStatus status;
}
