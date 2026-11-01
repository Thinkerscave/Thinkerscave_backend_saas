package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.enums.TimetableStatus;
import com.thinkerscave.academics.repository.*;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.service.TimetableService;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import com.thinkerscave.staff.repository.StaffRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
class AcademicsMeStudentTimetableTest {

    @Mock private AcademicsAccessGuard accessGuard;
    @Mock private AcademicYearRepository yearRepo;
    @Mock private StaffRepository staffRepo;
    @Mock private StudentRepository studentRepo;
    @Mock private StudentEnrollmentRepository enrollmentRepo;
    @Mock private TeacherAllocationTeacherRepository tatRepo;
    @Mock private SectionTeacherAssignmentRepository ctaRepo;
    @Mock private AcademicGroupSubjectMappingRepository mappingRepo;
    @Mock private TimetableVersionRepository versionRepo;
    @Mock private TimetableService timetableService;

    @InjectMocks private AcademicsMeServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesStudentTimetableYearFromActiveEnrollment() {
        long userId = 15L;
        long studentId = 58L;
        long enrollmentYearId = 2026L;
        authenticateStudent();
        Student student = student(studentId);
        StudentEnrollment enrollment = enrollment(enrollmentYearId);
        when(accessGuard.currentUserIdOrNull()).thenReturn(userId);
        when(studentRepo.findByUser_Id(userId)).thenReturn(Optional.of(student));
        when(enrollmentRepo.findActiveWithAcademicGroupByStudentId(studentId)).thenReturn(Optional.of(enrollment));
        when(versionRepo.findByAcademicYear_AcademicYearIdAndStatus(enrollmentYearId, TimetableStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        var response = service.getMyTimetable(null);

        assertEquals("STUDENT", response.getRole());
        assertEquals(enrollmentYearId, response.getAcademicYearId());
        verify(versionRepo).findByAcademicYear_AcademicYearIdAndStatus(
                enrollmentYearId, TimetableStatus.PUBLISHED);
    }

    @Test
    void rejectsStudentTimetableRequestsOutsideActiveEnrollmentYear() {
        long userId = 15L;
        long studentId = 58L;
        long enrollmentYearId = 2026L;
        authenticateStudent();
        when(accessGuard.currentUserIdOrNull()).thenReturn(userId);
        when(studentRepo.findByUser_Id(userId)).thenReturn(Optional.of(student(studentId)));
        when(enrollmentRepo.findActiveWithAcademicGroupByStudentId(studentId))
                .thenReturn(Optional.of(enrollment(enrollmentYearId)));

        assertThrows(AccessDeniedException.class, () -> service.getMyTimetable(2025L));
        verify(versionRepo, never()).findByAcademicYear_AcademicYearIdAndStatus(anyLong(), any());
    }

    private void authenticateStudent() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "student@example.test", null, List.of(new SimpleGrantedAuthority("STUDENT"))));
    }

    private Student student(long studentId) {
        Student student = new Student();
        student.setStudentId(studentId);
        return student;
    }

    private StudentEnrollment enrollment(long academicYearId) {
        AcademicYear year = new AcademicYear();
        year.setAcademicYearId(academicYearId);
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setAcademicYear(year);
        return enrollment;
    }
}
