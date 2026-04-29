package com.thinkerscave.common.student.controller;

import com.thinkerscave.common.student.dto.StudentRequestDTO;
import com.thinkerscave.common.student.dto.StudentResponseDTO;
import com.thinkerscave.common.student.service.StudentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import jakarta.validation.Valid;
import com.thinkerscave.common.commonModel.ApiResponse;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Student Management", description = "APIs for student registration and lifecycle")
@RequiredArgsConstructor
@Slf4j
public class StudentController {

    private final StudentService studentService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Register a new student with documents")
    @PostMapping(value = "/registerStudent")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN', 'STAFF') or hasAuthority('STUDENT_ADMISSIONS_ADD')")

    public ResponseEntity<ApiResponse<StudentResponseDTO>> uploadStudentWithDocuments(
            @Valid @RequestPart("studentData") StudentRequestDTO student,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestPart(value = "documents", required = false) List<MultipartFile> documents,
            @RequestPart(value = "types", required = false) List<String> types) throws IOException {

        // Boundary Validation for arrays
        if (documents != null && types != null && documents.size() != types.size()) {
            throw new IllegalArgumentException("The number of documents must match the number of document types.");
        }

        log.info("Received request to register student: {} {}", student.getFirstName(), student.getLastName());
        StudentResponseDTO studentSaved = studentService.saveStudentWithDocuments(student, photo, documents, types);
        return ResponseEntity.ok(ApiResponse.success("Student registered successfully", studentSaved));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get all students by organization")
    @GetMapping("/getStudents")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<com.thinkerscave.common.student.dto.StudentResponseDTO>>> getAllStudents() {
        log.info("Received request to get all students");
        return ResponseEntity.ok(ApiResponse.success(studentService.getAllStudents()));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get a student by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<com.thinkerscave.common.student.dto.StudentResponseDTO>> getStudentById(
            @PathVariable Long id) {
        log.info("Received request to get student by id: {}", id);
        return ResponseEntity.ok(ApiResponse.success(studentService.getStudentById(id)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Update an existing student")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<StudentResponseDTO>> updateStudent(
            @PathVariable Long id, 
            @Valid @RequestBody StudentRequestDTO dto) {
        log.info("Received request to update student by id: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Student updated successfully", studentService.updateStudent(id, dto)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Delete a student")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable Long id) {
        log.info("Received request to delete student by id: {}", id);
        studentService.deleteStudent(id);
        return ResponseEntity.ok(ApiResponse.success("Student deleted successfully", null));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get student documents")
    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN', 'STAFF') or hasAuthority('STUDENT_ADMISSIONS_VIEW')")
    public ResponseEntity<ApiResponse<List<com.thinkerscave.common.student.dto.StudentDocumentDTO>>> getStudentDocuments(
            @PathVariable Long id) {
        log.info("Received request to get documents for student id: {}", id);
        return ResponseEntity.ok(ApiResponse.success(studentService.getStudentDocuments(id)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Download a student document")
    @GetMapping("/document/{docId}/download")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN', 'STAFF') or hasAuthority('STUDENT_ADMISSIONS_VIEW')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long docId) {
        log.info("Received request to download document id: {}", docId);
        Resource file = studentService.downloadDocument(docId);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(file.getFilename() != null ? file.getFilename() : "document")
                .build();
                
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(file);
    }
}
