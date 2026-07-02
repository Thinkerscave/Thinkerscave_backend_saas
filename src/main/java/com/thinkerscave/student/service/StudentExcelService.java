package com.thinkerscave.student.service;

import java.io.ByteArrayInputStream;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.thinkerscave.student.dto.BulkUploadResponse;
import com.thinkerscave.student.dto.StudentImportJobResponse;

public interface StudentExcelService {

	ByteArrayInputStream downloadTemplate();

	StudentImportJobResponse importStudents(MultipartFile file);

	BulkUploadResponse getImportSummary(String jobId);

	Resource downloadValidationReport(String jobId);
}
