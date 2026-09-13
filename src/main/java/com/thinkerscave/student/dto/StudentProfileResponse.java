package com.thinkerscave.student.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentProfileResponse {

    private StudentResponseDTO student;

    private ParentDTO parent;

    private List<ParentDTO> parents;

    private EnrollmentDTO enrollment;

    private MedicalDTO medical;

    private PreviousSchoolingDTO previousSchooling;

    /** Student document id for PHOTO type — frontend loads via authenticated download. */
    private Long photoDocumentId;

    private List<TimelineDTO> timeline;
}