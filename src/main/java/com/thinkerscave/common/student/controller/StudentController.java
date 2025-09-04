package com.thinkerscave.common.student.controller;

import com.thinkerscave.common.student.domain.Student;
import com.thinkerscave.common.student.dto.StudentRequestDTO;
import com.thinkerscave.common.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
public class StudentController {

    @Autowired
    private StudentService studentService;
    @PostMapping(value = "/registerStudent")
    public ResponseEntity<Student> uploadStudentWithDocuments(
            @RequestPart("studentData") StudentRequestDTO student,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestPart(value = "documents", required = false) List<MultipartFile> documents,
            @RequestPart(value = "types", required = false) List<String> types) throws IOException {

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
