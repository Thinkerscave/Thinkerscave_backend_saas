package com.thinkerscave.student.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.shared.exceptions.FileProcessingException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.student.dto.BulkUploadResponse;
import com.thinkerscave.student.dto.StudentCreateRequest;
import com.thinkerscave.student.dto.StudentImportJobResponse;
import com.thinkerscave.student.service.StudentExcelService;
import com.thinkerscave.student.service.StudentService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StudentExcelServiceImpl implements StudentExcelService {

	private final Map<String, BulkUploadResponse> importJobs = new ConcurrentHashMap<>();
	private final Map<String, byte[]> validationReports = new ConcurrentHashMap<>();

	private final AcademicYearRepository academicYearRepository;
	private final ClassRepository classRepository;
	private final SectionRepository sectionRepository;
	private final StudentService studentService;

	// @Lazy breaks potential circular dependency via StudentService → UserService → ...
	public StudentExcelServiceImpl(AcademicYearRepository academicYearRepository,
			ClassRepository classRepository,
			SectionRepository sectionRepository,
			@Lazy StudentService studentService) {
		this.academicYearRepository = academicYearRepository;
		this.classRepository = classRepository;
		this.sectionRepository = sectionRepository;
		this.studentService = studentService;
	}

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
			int lastRowNum = sheet.getLastRowNum();
			log.info("Bulk import: sheet='{}' lastRowNum={}", sheet.getSheetName(), lastRowNum);
			int total = 0;
			int success = 0;
			int failed = 0;

			// Cache year/class lookups to avoid N+1 on each row
			Map<String, AcademicYear> yearCache = new ConcurrentHashMap<>();
			Map<String, AcademicClass> classCache = new ConcurrentHashMap<>();
			Map<String, AcademicSection> sectionCache = new ConcurrentHashMap<>();

			for (int i = 1; i <= lastRowNum; i++) {
				Row row = sheet.getRow(i);
				if (row == null || isBlankRow(row)) {
					continue;
				}

				total++;

				// ── Read columns ───────────────────────────────────────────
				String admissionNo   = readCell(row, 0);
				String firstName     = readCell(row, 1);
				String middleName    = readCell(row, 2);
				String lastName      = readCell(row, 3);
				String gender        = readCell(row, 4);
				String dobRaw        = readCell(row, 5);
				String mobile        = readCell(row, 6);
				String email         = readCell(row, 7);
				String yearCode      = readCell(row, 8);
				String classCode     = readCell(row, 9);
				String sectionCode   = readCell(row, 10);
				String rollNumber    = readCell(row, 11);
				String fatherName    = readCell(row, 12);
				String fatherMobile  = readCell(row, 13);
				String bloodGroup    = readCell(row, 17);
				String remarks       = readCell(row, 18);

				// ── Mandatory field check ──────────────────────────────────
				if (firstName.isBlank() || lastName.isBlank()
						|| yearCode.isBlank() || classCode.isBlank() || sectionCode.isBlank()
						|| fatherName.isBlank() || fatherMobile.isBlank()) {
					failed++;
					errors.add("Row " + (i + 1) + ": missing mandatory fields "
							+ "(First Name, Last Name, Academic Year, Class Code, Section Code, Father Name, Father Mobile)");
					continue;
				}

				// ── Resolve Academic Year ──────────────────────────────────
				AcademicYear year = yearCache.computeIfAbsent(yearCode, code ->
						academicYearRepository.findByYearCode(code)
								.orElse(null));
				if (year == null) {
					// Fallback: try current year
					year = academicYearRepository.findByCurrentYearTrue().orElse(null);
				}
				if (year == null) {
					failed++;
					errors.add("Row " + (i + 1) + ": academic year '" + yearCode + "' not found");
					continue;
				}

				final Long yearId = year.getAcademicYearId();

				// ── Resolve Class ──────────────────────────────────────────
				final String classCacheKey = yearId + "|" + classCode.toLowerCase();
				AcademicClass cls = classCache.computeIfAbsent(classCacheKey, k ->
						classRepository.findByAcademicYear_AcademicYearIdAndClassCodeIgnoreCase(yearId, classCode)
								.orElseGet(() ->
										classRepository.findByAcademicYear_AcademicYearIdAndClassNameIgnoreCase(yearId, classCode)
												.orElse(null)));
				if (cls == null) {
					failed++;
					errors.add("Row " + (i + 1) + ": class '" + classCode + "' not found for year '" + yearCode + "'");
					continue;
				}

				// ── Resolve Section ────────────────────────────────────────
				final String sectionCacheKey = cls.getClassId() + "|" + sectionCode.toLowerCase();
				AcademicSection section = sectionCache.computeIfAbsent(sectionCacheKey, k ->
						sectionRepository.findByAcademicClass_ClassIdAndSectionNameIgnoreCase(cls.getClassId(), sectionCode)
								.orElse(null));
				if (section == null) {
					failed++;
					errors.add("Row " + (i + 1) + ": section '" + sectionCode
							+ "' not found for class '" + classCode + "'");
					continue;
				}

				// ── Parse DOB ──────────────────────────────────────────────
				LocalDate dob = parseDob(dobRaw);
				if (dob == null) {
					failed++;
					errors.add("Row " + (i + 1) + ": invalid date of birth '" + dobRaw
							+ "' (expected yyyy-MM-dd or dd/MM/yyyy)");
					continue;
				}

				// ── Build request ──────────────────────────────────────────
				String[] fatherParts = splitName(fatherName);
				StudentCreateRequest req = new StudentCreateRequest();
				// Auto-generate admission number if not provided (matches frontend behavior)
				req.setAdmissionNumber(admissionNo.isBlank() ? "ADM-" + System.currentTimeMillis() : admissionNo);
				req.setFirstName(firstName);
				req.setMiddleName(middleName.isBlank() ? null : middleName);
				req.setLastName(lastName);
				req.setGender(gender.isBlank() ? "Male" : gender);
				req.setDateOfBirth(dob);
				req.setMobileNumber(mobile.isBlank() ? null : mobile);
				req.setEmail(email.isBlank() ? null : email);
				req.setRollNumber(rollNumber.isBlank() ? null : rollNumber);
				req.setBloodGroup(bloodGroup.isBlank() ? null : bloodGroup);
				req.setRemarks(remarks.isBlank() ? null : remarks);
				req.setAcademicYearId(year.getAcademicYearId());
				req.setClassId(cls.getClassId());
				req.setSectionId(section.getSectionId());
				req.setParentFirstName(fatherParts[0]);
				req.setParentLastName(fatherParts[1]);
				req.setParentMobileNumber(fatherMobile);

				// ── Persist ────────────────────────────────────────────────
				try {
					studentService.createStudent(req);
					success++;
					log.info("Bulk import row {}: created student {}", i + 1, firstName + " " + lastName);
				} catch (Exception ex) {
					failed++;
					String cause = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
					errors.add("Row " + (i + 1) + " (" + firstName + " " + lastName + "): " + cause);
					log.warn("Bulk import row {} failed: {}", i + 1, cause);
				}
			}

			summary.setTotalRecords(total);
			summary.setSuccessCount(success);
			summary.setFailureCount(failed);
			summary.setErrors(errors);

			importJobs.put(jobId, summary);
			String reportText = errors.isEmpty() ? "No errors — all records imported successfully."
					: String.join(System.lineSeparator(), errors);
			validationReports.put(jobId, reportText.getBytes(StandardCharsets.UTF_8));

			log.info("Bulk import complete: total={} success={} failed={}", total, success, failed);
			return StudentImportJobResponse.builder().jobId(jobId).summary(summary).build();

		} catch (IOException ex) {
			log.error("Failed to process student import file", ex);
			throw new FileProcessingException("Unable to process student import file", ex);
		}
	}

	/** Split a full name into [firstName, lastName]. */
	private String[] splitName(String fullName) {
		if (fullName == null || fullName.isBlank()) {
			return new String[] { "Unknown", "Unknown" };
		}
		int space = fullName.trim().indexOf(' ');
		if (space < 0) {
			return new String[] { fullName.trim(), fullName.trim() };
		}
		return new String[] {
				fullName.substring(0, space).trim(),
				fullName.substring(space + 1).trim()
		};
	}

	/** Parse date of birth from several common formats. */
	private LocalDate parseDob(String raw) {
		if (raw == null || raw.isBlank()) return null;
		List<DateTimeFormatter> formats = List.of(
				DateTimeFormatter.ISO_LOCAL_DATE,                          // yyyy-MM-dd
				DateTimeFormatter.ofPattern("dd/MM/yyyy"),
				DateTimeFormatter.ofPattern("d/M/yyyy"),
				DateTimeFormatter.ofPattern("MM/dd/yyyy"),
				DateTimeFormatter.ofPattern("dd-MM-yyyy")
		);
		for (DateTimeFormatter fmt : formats) {
			try { return LocalDate.parse(raw.trim(), fmt); } catch (DateTimeParseException ignored) {}
		}
		return null;
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
		Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
		if (cell == null) {
			return "";
		}
		return switch (cell.getCellType()) {
			case STRING -> {
				String v = cell.getStringCellValue();
				yield v != null ? v.trim() : "";
			}
			case NUMERIC -> {
				if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
					yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
				}
				double d = cell.getNumericCellValue();
				yield d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
			}
			case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
			case FORMULA -> {
				try {
					yield cell.getStringCellValue().trim();
				} catch (Exception e) {
					yield String.valueOf(cell.getNumericCellValue());
				}
			}
			default -> "";
		};
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
