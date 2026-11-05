package com.thinkerscave.student.controller;

import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.entity.StudentTimeline;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import com.thinkerscave.student.repository.StudentTimelineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentEnrollmentControllerTest {

    @Mock private StudentEnrollmentRepository enrollmentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private com.thinkerscave.academics.repository.AcademicYearRepository academicYearRepository;
    @Mock private com.thinkerscave.academics.repository.ClassRepository classRepository;
    @Mock private com.thinkerscave.academics.repository.SectionRepository sectionRepository;
    @Mock private StudentTimelineRepository timelineRepository;

    @InjectMocks private StudentEnrollmentController controller;

    @Test
    void reassignsCurrentYearPlacementAndPreservesPreviousEnrollment() {
        Student student = new Student();
        student.setStudentId(7L);
        AcademicYear year = new AcademicYear();
        year.setAcademicYearId(4L);
        AcademicClass academicClass = new AcademicClass();
        academicClass.setClassId(8L);
        academicClass.setAcademicYear(year);
        academicClass.setActive(true);
        AcademicClass previousClass = new AcademicClass();
        previousClass.setClassId(3L);
        previousClass.setAcademicYear(year);
        StudentEnrollment current = new StudentEnrollment();
        current.setStudent(student);
        current.setAcademicYear(year);
        current.setClassEntity(previousClass);
        current.setActive(true);
        when(studentRepository.findById(7L)).thenReturn(Optional.of(student));
        when(academicYearRepository.findById(4L)).thenReturn(Optional.of(year));
        when(classRepository.findById(8L)).thenReturn(Optional.of(academicClass));
        when(enrollmentRepository.findByStudentStudentIdAndActiveTrue(7L)).thenReturn(Optional.of(current));
        when(enrollmentRepository.save(any(StudentEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentEnrollmentController.EnrollmentUpdateRequest request = new StudentEnrollmentController.EnrollmentUpdateRequest();
        request.setAcademicYearId(4L);
        request.setClassId(8L);
        request.setRollNumber("R-12");

        var response = controller.updateEnrollment(7L, request);

        assertThat(response.getBody().getData().getRollNumber()).isEqualTo("R-12");
        assertThat(current.getActive()).isFalse();
        assertThat(current.getStatus()).isEqualTo(com.thinkerscave.student.enums.EnrollmentStatus.REASSIGNED);
        org.mockito.ArgumentCaptor<StudentEnrollment> savedEnrollments =
            org.mockito.ArgumentCaptor.forClass(StudentEnrollment.class);
        verify(enrollmentRepository, org.mockito.Mockito.times(2)).save(savedEnrollments.capture());
        assertThat(savedEnrollments.getAllValues().get(1).getClassEntity()).isSameAs(academicClass);
        assertThat(savedEnrollments.getAllValues().get(1).getActive()).isTrue();
        verify(timelineRepository).save(any(StudentTimeline.class));
        verify(enrollmentRepository, never()).existsByStudentStudentIdAndAcademicYearAcademicYearId(any(), any());
    }

    @Test
    void missingAcademicYearDoesNotDeactivateCurrentEnrollment() {
        when(studentRepository.findById(7L)).thenReturn(Optional.of(new Student()));
        when(academicYearRepository.findById(4L)).thenReturn(Optional.empty());

        StudentEnrollmentController.EnrollmentUpdateRequest request = new StudentEnrollmentController.EnrollmentUpdateRequest();
        request.setAcademicYearId(4L);
        request.setClassId(8L);

        assertThatThrownBy(() -> controller.updateEnrollment(7L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(enrollmentRepository, never()).save(any());
    }
}
