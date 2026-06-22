package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubjectRequest {

    @NotBlank(message = "Subject code is mandatory")
    @Size(max = 30)
    private String subjectCode;

    @NotBlank(message = "Subject name is mandatory")
    @Size(max = 100)
    private String subjectName;

    private String subjectType;
    private Boolean active = true;
    private String remarks;
}
