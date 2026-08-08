package com.thinkerscave.student.controller;

import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.student.dto.StudentDocumentDTO;
import com.thinkerscave.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Student Documents")
@RequiredArgsConstructor
public class StudentDocumentController {

    private final StudentService studentService;

    @GetMapping
    @Operation(summary = "List student documents")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<List<StudentDocumentDTO>>> list(@RequestParam("studentId") Long studentId) {
        return ResponseEntity.ok(ApiResponse.success("Documents loaded", studentService.getStudentDocuments(studentId)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload student document")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<StudentDocumentDTO>> upload(
            @RequestParam("studentId") Long studentId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "documentType", required = false) String documentType) throws IOException {
        StudentDocumentDTO dto = studentService.uploadStudentDocument(studentId, file, documentType);
        return ResponseEntity.status(201).body(ApiResponse.success("Document uploaded", dto));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download document")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Resource file = studentService.downloadDocument(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.getFilename() == null ? "document" : file.getFilename())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete document")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        studentService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success("Document deleted", null));
    }
}
