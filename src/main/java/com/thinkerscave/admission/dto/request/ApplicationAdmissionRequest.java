package com.thinkerscave.admission.dto.request;

import com.thinkerscave.admission.dto.ApplicationProfileDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Submit or draft a student admission application")
public class ApplicationAdmissionRequest {

    @NotBlank(message = "Applicant name is required")
    private String applicantName;

    private LocalDate dateOfBirth;
    private String gender;
    private String applyingForClass;
    private Long academicYearId;
    private Long classId;
    private Long sectionId;
    private String email;
    private String contactNumber;
    private String address;
    private String parentName;
    private String parentContact;
    private String parentEmail;
    private String internalComments;
    private Long inquiryId;
    private ApplicationProfileDetails profile;
}
