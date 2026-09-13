package com.thinkerscave.student.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreviousSchoolingDTO {
    private Boolean hasPreviousSchooling;
    private String schoolName;
    private String board;
    private String className;
    private String academicYear;
    private String percentage;
    private String tcNumber;
    private String tcDate;
}
