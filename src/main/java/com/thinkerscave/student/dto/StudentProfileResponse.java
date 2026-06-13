package com.thinkerscave.student.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentProfileResponse {

    private StudentResponseDTO student;

    private ParentDTO parent;

    private EnrollmentDTO enrollment;

    private MedicalDTO medical;

    private List<TimelineDTO> timeline;
}