package com.thinkerscave.student.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.thinkerscave.shared.exceptions.FileProcessingException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.student.dto.BulkUploadResponse;
import com.thinkerscave.student.dto.StudentImportJobResponse;
import com.thinkerscave.student.service.StudentExcelService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StudentExcelServiceImpl implements StudentExcelService {

	private final Map<String, BulkUploadResponse> importJobs = new ConcurrentHashMap<>();
	private final Map<String, byte[]> validationReports = new ConcurrentHashMap<>();

	@Override
	public ByteArrayInputStream downloadTemplate() {

		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

			Sheet templateSheet = workbook.createSheet("Student Import");
			Sheet instructionSheet = workbook.createSheet("Instructions");

			createStudentImportHeader(templateSheet);
			createInstructionSheet(instructionSheet);

			workbook.write(outputStream);

			return new ByteArrayInputStream(outputStream.toByteArray());

		} catch (IOException ex) {

			log.error("Failed to generate student import template", ex);

			throw new FileProcessingException("Unable to generate student import template", ex);
		}
	}

	private void createStudentImportHeader(Sheet sheet) {

		Row header = sheet.createRow(0);

		String[] columns = { "Admission Number *", "First Name *", "Middle Name", "Last Name *", "Gender *",
				"Date Of Birth *", "Mobile Number", "Email", "Academic Year *", "Class Code *", "Section Code *",
				"Roll Number", "Father Name *", "Father Mobile *", "Mother Name", "Mother Mobile", "Current Address",
				"Blood Group", "Remarks" };

		for (int i = 0; i < columns.length; i++) {
			Cell cell = header.createCell(i);
			cell.setCellValue(columns[i]);
			sheet.autoSizeColumn(i);
		}
	}

	private void createInstructionSheet(Sheet sheet) {

		Row header = sheet.createRow(0);

		header.createCell(0).setCellValue("Field");
		header.createCell(1).setCellValue("Mandatory");
		header.createCell(2).setCellValue("Description");

		createInstructionRow(sheet, 1, "Admission Number", "Yes", "Unique student admission number");

		createInstructionRow(sheet, 2, "Gender", "Yes", "Allowed values: MALE,FEMALE,OTHER");

		createInstructionRow(sheet, 3, "Date Of Birth", "Yes", "Format: dd-MM-yyyy");

		createInstructionRow(sheet, 4, "Blood Group", "No", "A+,A-,B+,B-,AB+,AB-,O+,O-");

		createInstructionRow(sheet, 5, "Class Code", "Yes", "Must exist in system");

		createInstructionRow(sheet, 6, "Section Code", "Yes", "Must exist in system");
	}

	private void createInstructionRow(Sheet sheet, int rowNumber, String field, String mandatory, String description) {

		Row row = sheet.createRow(rowNumber);

		row.createCell(0).setCellValue(field);
		row.createCell(1).setCellValue(mandatory);
		row.createCell(2).setCellValue(description);
	}

	@Override
	public StudentImportJobResponse importStudents(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new FileProcessingException("Import file is empty");
		}

		String jobId = UUID.randomUUID().toString();
		BulkUploadResponse summary = new BulkUploadResponse();
		List<String> errors = new ArrayList<>();

		try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
			Sheet sheet = workbook.getSheetAt(0);
			int total = 0;
			int success = 0;
			int failed = 0;

			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				if (row == null || isBlankRow(row)) {
					continue;
				}

				total++;
				String admissionNo = readCell(row, 0);
				String firstName = readCell(row, 1);
				String lastName = readCell(row, 3);
				String academicYear = readCell(row, 8);
				String classCode = readCell(row, 9);
				String sectionCode = readCell(row, 10);

				if (admissionNo.isBlank() || firstName.isBlank() || lastName.isBlank()
						|| academicYear.isBlank() || classCode.isBlank() || sectionCode.isBlank()) {
					failed++;
					errors.add("Row " + (i + 1) + ": missing mandatory fields");
					continue;
				}

				success++;
			}

			summary.setTotalRecords(total);
			summary.setSuccessCount(success);
			summary.setFailureCount(failed);
			summary.setErrors(errors);

			importJobs.put(jobId, summary);
			String reportText = errors.isEmpty() ? "No validation errors." : String.join(System.lineSeparator(), errors);
			validationReports.put(jobId, reportText.getBytes(StandardCharsets.UTF_8));

			return StudentImportJobResponse.builder().jobId(jobId).summary(summary).build();
		} catch (IOException ex) {
			log.error("Failed to process student import file", ex);
			throw new FileProcessingException("Unable to process student import file", ex);
		}
	}

	@Override
	public BulkUploadResponse getImportSummary(String jobId) {
		BulkUploadResponse response = importJobs.get(jobId);
		if (response == null) {
			throw new ResourceNotFoundException("Import job not found: " + jobId);
		}
		return response;
	}

	@Override
	public Resource downloadValidationReport(String jobId) {
		byte[] content = validationReports.get(jobId);
		if (content == null) {
			throw new ResourceNotFoundException("Validation report not found for job: " + jobId);
		}
		return new ByteArrayResource(content);
	}

	private String readCell(Row row, int index) {
		Cell cell = row.getCell(index);
		if (cell == null) {
			return "";
		}
		cell.setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
		return cell.getStringCellValue() != null ? cell.getStringCellValue().trim() : "";
	}

	private boolean isBlankRow(Row row) {
		for (int i = 0; i <= row.getLastCellNum(); i++) {
			String value = readCell(row, i);
			if (!value.isBlank()) {
				return false;
			}
		}
		return true;
	}

}
