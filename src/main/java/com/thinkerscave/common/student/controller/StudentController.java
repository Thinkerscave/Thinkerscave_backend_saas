package com.thinkerscave.common.student.controller;

import com.thinkerscave.common.student.domain.Student;
import com.thinkerscave.common.student.dto.StudentRequestDTO;
import com.thinkerscave.common.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@CrossOrigin("*")
@Slf4j
public class StudentController {

    private final StudentService studentService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Register a new student with documents", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @PostMapping(value = "/registerStudent")

    public ResponseEntity<Student> uploadStudentWithDocuments(
            @RequestPart("studentData") StudentRequestDTO student,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestPart(value = "documents", required = false) List<MultipartFile> documents,
            @RequestPart(value = "types", required = false) List<String> types) throws IOException {

        log.info("Received request to register student: {} {}", student.getFirstName(), student.getLastName());
        Student studentSaved = studentService.saveStudentWithDocuments(student, photo, documents, types);
        if (studentSaved != null) {
            return ResponseEntity.ok(studentSaved);
        } else {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Student());
        }
    }
}
