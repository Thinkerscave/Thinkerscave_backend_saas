package com.thinkerscave.student.service.impl;

import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.student.dto.BulkUploadResponse;
import com.thinkerscave.student.dto.StudentCreateRequest;
import com.thinkerscave.student.dto.StudentImportJobResponse;
import com.thinkerscave.student.entity.StudentImportJob;
import com.thinkerscave.student.repository.StudentImportJobRepository;
import com.thinkerscave.student.repository.StudentRepository;
import com.thinkerscave.student.service.StudentService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentExcelServiceImplTest {

    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private ClassRepository classRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private StudentService studentService;
    @Mock private StudentRepository studentRepository;
    @Mock private StudentImportJobRepository studentImportJobRepository;

    private StudentExcelServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StudentExcelServiceImpl(academicYearRepository, classRepository, sectionRepository,
                studentRepository, studentImportJobRepository, studentService);
    }

    @Test
    void previewValidatesRowsAndDuplicateAdmissionNumbersWithoutCreatingStudents() throws Exception {
        mockLookups();
        when(studentRepository.existsByAdmissionNumber("ADM-1")).thenReturn(false);

        BulkUploadResponse preview = service.previewStudents(workbook(
                row("ADM-1", "Asha", "Rao"),
                row("ADM-1", "Maya", "Das")));

        assertThat(preview.getTotalRecords()).isEqualTo(2);
        assertThat(preview.getSuccessCount()).isEqualTo(1);
        assertThat(preview.getFailureCount()).isEqualTo(1);
        assertThat(preview.getErrors()).singleElement().asString().contains("Row 3", "duplicated");
        verify(studentService, never()).createStudent(any(StudentCreateRequest.class));
        verify(studentImportJobRepository, never()).save(any());
    }

    @Test
    void completedImportJobAndErrorReportArePersisted() throws Exception {
        mockLookups();
        when(studentRepository.existsByAdmissionNumber("ADM-2")).thenReturn(false);
        when(studentService.createStudent(any(StudentCreateRequest.class)))
                .thenReturn(new com.thinkerscave.student.dto.StudentResponseDTO());
        when(studentImportJobRepository.save(any(StudentImportJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentImportJobResponse result = service.importStudents(workbook(row("ADM-2", "Lina", "Sen")));

        ArgumentCaptor<StudentImportJob> saved = ArgumentCaptor.forClass(StudentImportJob.class);
        verify(studentImportJobRepository).save(saved.capture());
        assertThat(result.getJobId()).isEqualTo(saved.getValue().getJobId());
        assertThat(saved.getValue().getStatus()).isEqualTo("COMPLETED");
        assertThat(saved.getValue().getSuccessCount()).isEqualTo(1);
        when(studentImportJobRepository.findById(result.getJobId())).thenReturn(Optional.of(saved.getValue()));
        assertThat(service.getImportSummary(result.getJobId()).getSuccessCount()).isEqualTo(1);
        assertThat(service.downloadValidationReport(result.getJobId()).getContentAsByteArray())
                .asString(java.nio.charset.StandardCharsets.UTF_8).contains("No errors");
    }

    private void mockLookups() {
        AcademicYear year = new AcademicYear();
        year.setAcademicYearId(10L);
        year.setName("2026-27");
        AcademicClass academicClass = new AcademicClass();
        academicClass.setClassId(20L);
        AcademicSection section = new AcademicSection();
        section.setSectionId(30L);
        when(academicYearRepository.findByYearCode("2026-27")).thenReturn(Optional.of(year));
        when(classRepository.findByAcademicYear_AcademicYearIdAndClassCodeIgnoreCase(10L, "C6"))
                .thenReturn(Optional.of(academicClass));
        when(sectionRepository.findByAcademicClass_ClassIdAndSectionNameIgnoreCase(20L, "A"))
                .thenReturn(Optional.of(section));
    }

    private static MockMultipartFile workbook(String[]... rows) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Students");
            String[] headers = {"Admission Number", "First Name", "Middle Name", "Last Name", "Gender",
                    "Date Of Birth", "Mobile", "Email", "Academic Year", "Class Code", "Section Code",
                    "Roll Number", "Father Name", "Father Mobile", "Mother Name", "Mother Mobile",
                    "Address", "Blood Group", "Remarks"};
            Row header = sheet.createRow(0);
            for (int col = 0; col < headers.length; col++) header.createCell(col).setCellValue(headers[col]);
            for (int index = 0; index < rows.length; index++) {
                Row row = sheet.createRow(index + 1);
                for (int col = 0; col < rows[index].length; col++) row.createCell(col).setCellValue(rows[index][col]);
            }
            workbook.write(output);
            return new MockMultipartFile("file", "students.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }

    private static String[] row(String admissionNumber, String firstName, String lastName) {
        return new String[]{admissionNumber, firstName, "", lastName, "FEMALE", "2015-05-10", "", "",
                "2026-27", "C6", "A", "", "Mira Rao", "9876543210", "", "", "", "", ""};
    }
}
