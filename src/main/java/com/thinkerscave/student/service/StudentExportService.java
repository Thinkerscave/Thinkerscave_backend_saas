package com.thinkerscave.student.service;

import com.thinkerscave.student.dto.StudentResponseDTO;
import com.thinkerscave.student.dto.StudentSearchRequest;
import com.thinkerscave.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentExportService {

    private static final int EXPORT_PAGE_SIZE = 500;
    private static final String[] HEADERS = {
            "Student ID", "Student Code", "Admission Number", "First Name", "Middle Name", "Last Name",
            "Status", "Academic Year", "Class", "Section", "Roll Number", "Parent / Guardian",
            "Parent Mobile", "Student Mobile", "Email"
    };

    private final StudentService studentService;

    @Transactional(readOnly = true)
    public byte[] exportCsv(StudentSearchRequest request) {
        List<StudentResponseDTO> rows = new ArrayList<>();
        Page<StudentResponseDTO> page;
        int pageNumber = 0;
        do {
            Pageable pageable = PageRequest.of(pageNumber++, EXPORT_PAGE_SIZE,
                    Sort.by(Sort.Direction.ASC, "firstName").and(Sort.by(Sort.Direction.ASC, "studentId")));
            page = studentService.searchStudents(request, pageable);
            rows.addAll(page.getContent());
        } while (page.hasNext());

        StringBuilder csv = new StringBuilder("\uFEFF");
        appendRow(csv, HEADERS);
        for (StudentResponseDTO student : rows) {
            appendRow(csv, new String[]{
                    value(student.getStudentId()), value(student.getStudentCode()), value(student.getAdmissionNumber()),
                    value(student.getFirstName()), value(student.getMiddleName()), value(student.getLastName()),
                    value(student.getStatus()), value(student.getAcademicYear()), value(student.getClassName()), value(student.getSectionName()),
                    value(student.getRollNumber()), value(student.getParentName()), value(student.getParentMobileNumber()),
                    value(student.getMobileNumber()), value(student.getEmail())
            });
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendRow(StringBuilder csv, String[] values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) csv.append(',');
            String value = values[index] == null ? "" : values[index];
            if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) {
                value = "'" + value;
            }
            csv.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }

    private static String value(Object value) {
        return value == null ? "" : value.toString();
    }
}