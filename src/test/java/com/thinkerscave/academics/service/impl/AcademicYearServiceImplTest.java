package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.response.AcademicYearResponse;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.TimetableConfiguration;
import com.thinkerscave.academics.enums.AcademicYearPattern;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.repository.AcademicGroupRepository;
import com.thinkerscave.academics.repository.AcademicGroupSubjectMappingRepository;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.academics.repository.SubjectRepository;
import com.thinkerscave.academics.repository.TeacherAllocationRepository;
import com.thinkerscave.academics.repository.TimetableConfigurationRepository;
import com.thinkerscave.academics.repository.TimetableVersionRepository;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicYearServiceImplTest {

    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private AcademicGroupRepository academicGroupRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private AcademicGroupSubjectMappingRepository academicGroupSubjectMappingRepository;
    @Mock private TeacherAllocationRepository teacherAllocationRepository;
    @Mock private TimetableConfigurationRepository timetableConfigurationRepository;
    @Mock private TimetableVersionRepository timetableVersionRepository;
    @Mock private StudentEnrollmentRepository studentEnrollmentRepository;
    @Mock private AcademicsAccessGuard accessGuard;

    @InjectMocks private AcademicYearServiceImpl service;

    @BeforeEach
    void setupCommonStubs() {
        when(academicGroupRepository.findByAcademicYear_AcademicYearIdOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of());
        when(academicGroupRepository.findByAcademicYear_AcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of());
        when(subjectRepository.count()).thenReturn(0L);
        when(subjectRepository.countByActiveTrue()).thenReturn(1L);
        when(studentEnrollmentRepository.countByAcademicYearAcademicYearIdAndActiveTrue(1L)).thenReturn(0L);
        when(teacherAllocationRepository.findBySection_AcademicGroup_AcademicYear_AcademicYearId(1L))
                .thenReturn(List.of());
        when(timetableVersionRepository.findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(1L))
                .thenReturn(List.of());
    }

    @Test
    void readinessMarksTimetableConfigurationCompleteWhenConfigExists() {
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(sampleYear()));
        when(timetableConfigurationRepository.findByAcademicYear_AcademicYearId(1L))
                .thenReturn(List.of(new TimetableConfiguration()));

        AcademicYearResponse response = service.getById(1L);

        assertNotNull(response.getReadinessSteps());
        AcademicYearResponse.ReadinessStep configStep = response.getReadinessSteps().stream()
                .filter(step -> "TIMETABLE_CONFIG".equals(step.getCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("COMPLETE", configStep.getState());
    }

    @Test
    void readinessExcludesStudentPromotionPlanStep() {
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(sampleYear()));
        when(timetableConfigurationRepository.findByAcademicYear_AcademicYearId(1L))
                .thenReturn(List.of());

        AcademicYearResponse response = service.getById(1L);

        assertTrue(response.getReadinessSteps().stream()
                .noneMatch(step -> "STUDENT_PROMOTION".equals(step.getCode())));
        assertFalse(response.getReadinessSteps().isEmpty());

        AcademicYearResponse.ReadinessStep approvalStep = response.getReadinessSteps().stream()
                .filter(step -> "FINAL_APPROVAL".equals(step.getCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("COMPLETE", approvalStep.getState());

        AcademicYearResponse.ReadinessStep activationStep = response.getReadinessSteps().stream()
                .filter(step -> "ACTIVATION".equals(step.getCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("COMPLETE", activationStep.getState());
    }

    private AcademicYear sampleYear() {
        AcademicYear year = new AcademicYear();
        year.setAcademicYearId(1L);
        year.setName("2026");
        year.setPattern(AcademicYearPattern.ANNUAL);
        year.setStatus(AcademicYearStatus.APPROVED);
        year.setActive(true);
        year.setStartDate(LocalDate.now().minusDays(10));
        year.setEndDate(LocalDate.now().plusDays(10));
        return year;
    }
}
