package com.thinkerscave.student.service;

import java.io.ByteArrayInputStream;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StudentExcelService {

	ByteArrayInputStream downloadTemplate();

    StudentImportResponse importStudents(
            MultipartFile file);
}
