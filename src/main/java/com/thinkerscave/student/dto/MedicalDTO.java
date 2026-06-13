package com.thinkerscave.student.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MedicalDTO {

    private String bloodGroup;

    private String allergies;

    private String medicalConditions;

    private String medications;

    private String doctorName;

    private String doctorContact;

    private String emergencyNotes;
}