package com.thinkerscave.admission.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkerscave.admission.dto.AdmissionGuardianDetails;
import com.thinkerscave.admission.dto.ApplicationProfileDetails;
import com.thinkerscave.admission.dto.request.EnrollApplicationRequest;
import com.thinkerscave.admission.entity.ApplicationAdmission;
import com.thinkerscave.student.dto.StudentCreateRequest;
import com.thinkerscave.student.dto.StudentParentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationAdmissionStudentConversionTest {

    @Mock private ObjectMapper objectMapper;

    @InjectMocks private ApplicationAdmissionServiceImpl service;

    @Test
    void admissionConversionPreservesEveryGuardianAndChoosesExactlyOnePrimary() throws Exception {
        ApplicationAdmission application = new ApplicationAdmission();
        application.setApplicantName("Asha Rao");
        application.setParentName("Mira Rao");
        application.setParentContact("9876543210");
        application.setDateOfBirth(LocalDate.of(2015, 5, 10));
        application.setGender("FEMALE");
        application.setProfileDetails("{}");
        ApplicationProfileDetails profile = new ApplicationProfileDetails();
        profile.setSocialCategory("OBC");
        profile.setAddressLine1("12 Main Road");
        profile.setLandmark("Near Central Park");
        profile.setDistrict("Pune District");
        profile.setGuardians(List.of(
                new AdmissionGuardianDetails("Mira Rao", "MOTHER", "9876543210", "mira@example.test", "Teacher", true),
                new AdmissionGuardianDetails("Raj Rao", "FATHER", "9876543211", "raj@example.test", "Engineer", false),
                new AdmissionGuardianDetails("Neha Das", "GUARDIAN", "9876543212", null, null, false)));
        when(objectMapper.readValue(anyString(), eq(ApplicationProfileDetails.class))).thenReturn(profile);

        EnrollApplicationRequest enrollment = new EnrollApplicationRequest();
        enrollment.setAcademicYearId(4L);
        enrollment.setClassId(8L);

        StudentCreateRequest request = ReflectionTestUtils.invokeMethod(
                service, "toStudentCreateRequest", application, enrollment, "ADM-2026-1");

        assertThat(request.getAdmissionNumber()).isEqualTo("ADM-2026-1");
        assertThat(request.getSocialCategory()).isEqualTo("OBC");
        assertThat(request.getCurrentLandmark()).isEqualTo("Near Central Park");
        assertThat(request.getCurrentDistrict()).isEqualTo("Pune District");
        assertThat(request.getParents()).hasSize(3);
        assertThat(request.getParents()).extracting(StudentParentRequest::getPrimaryContact)
                .containsExactly(true, false, false);
        assertThat(request.getParents()).extracting(StudentParentRequest::getMobileNumber)
                .containsExactly("9876543210", "9876543211", "9876543212");
    }
}
