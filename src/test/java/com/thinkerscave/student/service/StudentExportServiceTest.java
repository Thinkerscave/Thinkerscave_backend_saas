package com.thinkerscave.student.service;

import com.thinkerscave.student.dto.StudentResponseDTO;
import com.thinkerscave.student.dto.StudentSearchRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentExportServiceTest {

    @Mock private StudentService studentService;

    @InjectMocks private StudentExportService exportService;

    @Test
    void exportsAcademicYearFromActiveEnrollment() {
        StudentResponseDTO student = new StudentResponseDTO();
        student.setStudentId(15L);
        student.setFirstName("Asha");
        student.setLastName("Rao");
        student.setStatus("ACTIVE");
        student.setAcademicYear("2026-27");
        student.setClassName("Class 6");
        when(studentService.searchStudents(any(StudentSearchRequest.class), any()))
                .thenReturn(new PageImpl<>(List.of(student), PageRequest.of(0, 500), 1));

        String csv = new String(exportService.exportCsv(new StudentSearchRequest()), StandardCharsets.UTF_8);

        assertThat(csv).contains("\"ACTIVE\",\"2026-27\",\"Class 6\"");
    }
}
