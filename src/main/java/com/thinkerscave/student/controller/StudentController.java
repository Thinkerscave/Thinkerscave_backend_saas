package com.thinkerscave.student.controller;

import com.thinkerscave.student.dto.StudentCreateRequest;
import com.thinkerscave.student.dto.StudentImportJobResponse;
import com.thinkerscave.student.dto.StudentResponseDTO;
import com.thinkerscave.student.dto.StudentSearchRequest;
import com.thinkerscave.student.dto.StudentStatusUpdateRequest;
import com.thinkerscave.student.dto.TimelineCreateRequest;
import com.thinkerscave.student.dto.TimelineDTO;
import com.thinkerscave.student.enums.StudentStatus;
import com.thinkerscave.student.service.StudentExcelService;
import com.thinkerscave.student.service.StudentService;
import com.thinkerscave.academics.dto.response.LookupDTO;
import com.thinkerscave.academics.service.AcademicsLookupService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import jakarta.validation.Valid;
import com.thinkerscave.shared.dto.ApiResponse;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Student Management", description = "APIs for student registration and lifecycle")
@RequiredArgsConstructor
@Slf4j
public class StudentController {

	private final StudentService studentService;

	private final StudentExcelService studentExcelService;

	private final AcademicsLookupService academicsLookupService;

	@GetMapping("/import/template")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF')")
	@Operation(summary = "Download Student Import Template")
	public ResponseEntity<Resource> downloadStudentImportTemplate() {

		ByteArrayInputStream stream = studentExcelService.downloadTemplate();

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student_import_template.xlsx")
				.contentType(
						MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.body(new InputStreamResource(stream));
	}

	@io.swagger.v3.oas.annotations.Operation(summary = "Register a new student with documents")
	@PostMapping(value = "/registerStudent")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF') or hasAuthority('STUDENT_ADMISSIONS_ADD')")

	public ResponseEntity<ApiResponse<StudentResponseDTO>> uploadStudentWithDocuments(
			@Valid @RequestPart("studentData") StudentCreateRequest student,
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

	@io.swagger.v3.oas.annotations.Operation(summary = "Register a new student without file uploads")
	@PostMapping
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF') or hasAuthority('STUDENT_ADMISSIONS_ADD')")
	public ResponseEntity<ApiResponse<StudentResponseDTO>> createStudent(
			@Valid @RequestBody StudentCreateRequest student) throws IOException {
		log.info("Received JSON request to register student: {} {}", student.getFirstName(), student.getLastName());
		StudentResponseDTO studentSaved = studentService.createStudent(student);
		return ResponseEntity.ok(ApiResponse.success("Student registered successfully", studentSaved));
	}

	@io.swagger.v3.oas.annotations.Operation(summary = "Get all students by organization")
	@GetMapping("/getStudents")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF','TEACHER')")
	public ResponseEntity<ApiResponse<List<StudentResponseDTO>>> getAllStudents() {
		log.info("Received request to get all students");
		return ResponseEntity.ok(ApiResponse.success(studentService.getAllStudents()));
	}

	@io.swagger.v3.oas.annotations.Operation(summary = "Get students (paginated)")
	@GetMapping("/paged")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF','TEACHER')")
	public ResponseEntity<ApiResponse<Page<StudentResponseDTO>>> getAllStudentsPaged(Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success(studentService.getAllStudents(pageable)));
	}

	@Operation(summary = "Student directory with pagination and filters")
	@GetMapping
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF','TEACHER')")
	public ResponseEntity<ApiResponse<Page<StudentResponseDTO>>> getDirectory(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long classId,
			@RequestParam(required = false) Long sectionId,
			@RequestParam(required = false) StudentStatus status,
			@RequestParam(required = false) String parentName,
			Pageable pageable) {
		StudentSearchRequest request = new StudentSearchRequest();
		request.setKeyword(keyword);
		request.setClassId(classId);
		request.setSectionId(sectionId);
		request.setStatus(status);
		request.setParentName(parentName);
		return ResponseEntity.ok(ApiResponse.success("Student directory loaded", studentService.searchStudents(request, pageable)));
	}

	@Operation(summary = "Advanced student search")
	@PostMapping("/search")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF','TEACHER')")
	public ResponseEntity<ApiResponse<Page<StudentResponseDTO>>> searchStudents(
			@RequestBody(required = false) StudentSearchRequest request,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("Student search completed", studentService.searchStudents(request, pageable)));
	}

	@io.swagger.v3.oas.annotations.Operation(summary = "Get a student by ID")
	@GetMapping("/{id}")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF','TEACHER')")
	public ResponseEntity<ApiResponse<StudentResponseDTO>> getStudentById(@PathVariable Long id) {
		log.info("Received request to get student by id: {}", id);
		return ResponseEntity.ok(ApiResponse.success(studentService.getStudentById(id)));
	}

	@io.swagger.v3.oas.annotations.Operation(summary = "Update an existing student")
	@PutMapping("/{id}")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF')")
	public ResponseEntity<ApiResponse<StudentResponseDTO>> updateStudent(@PathVariable Long id,
			@Valid @RequestBody StudentCreateRequest dto) {
		log.info("Received request to update student by id: {}", id);
		return ResponseEntity
				.ok(ApiResponse.success("Student updated successfully", studentService.updateStudent(id, dto)));
	}

	@io.swagger.v3.oas.annotations.Operation(summary = "Delete a student")
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF')")
	public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable Long id) {
		log.info("Received request to delete student by id: {}", id);
		studentService.deleteStudent(id);
		return ResponseEntity.ok(ApiResponse.success("Student deleted successfully", null));
	}

	@Operation(summary = "Update student status")
	@PatchMapping("/{id}/status")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF')")
	public ResponseEntity<ApiResponse<StudentResponseDTO>> updateStudentStatus(
			@PathVariable Long id,
			@Valid @RequestBody StudentStatusUpdateRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Student status updated",
				studentService.updateStudentStatus(id, request.getStatus())));
	}

	@io.swagger.v3.oas.annotations.Operation(summary = "Get Student Profile 360")
	@GetMapping("/{id}/profile-360")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF','TEACHER') or hasAuthority('STUDENT_ADMISSIONS_VIEW')")
	public ResponseEntity<ApiResponse<com.thinkerscave.student.dto.StudentProfileResponse>> getProfile360(
			@PathVariable Long id) {
		log.info("Received request to get profile 360 for student id: {}", id);
		return ResponseEntity.ok(ApiResponse.success(studentService.getProfile360(id)));
	}

	@io.swagger.v3.oas.annotations.Operation(summary = "Update personal information")
	@PutMapping("/{id}/personal")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF')")
	public ResponseEntity<ApiResponse<StudentResponseDTO>> updatePersonal(@PathVariable Long id,
			@Valid @RequestBody StudentCreateRequest dto) {
		log.info("Received request to update personal info for student id: {}", id);
		return ResponseEntity
				.ok(ApiResponse.success("Personal info updated successfully", studentService.updatePersonal(id, dto)));
	}

	@io.swagger.v3.oas.annotations.Operation(summary = "Update medical information")
	@PutMapping("/{id}/medical")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF')")
	public ResponseEntity<ApiResponse<StudentResponseDTO>> updateMedical(@PathVariable Long id,
			@Valid @RequestBody com.thinkerscave.student.dto.MedicalDTO dto) {
		log.info("Received request to update medical info for student id: {}", id);
		return ResponseEntity
				.ok(ApiResponse.success("Medical info updated successfully", studentService.updateMedical(id, dto)));
	}

	@io.swagger.v3.oas.annotations.Operation(summary = "Get student timeline")
	@GetMapping("/{id}/timeline")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF','TEACHER') or hasAuthority('STUDENT_ADMISSIONS_VIEW')")
	public ResponseEntity<ApiResponse<List<com.thinkerscave.student.dto.TimelineDTO>>> getTimeline(
			@PathVariable Long id) {
		log.info("Received request to get timeline for student id: {}", id);
		return ResponseEntity.ok(ApiResponse.success(studentService.getTimeline(id)));
	}

	@Operation(summary = "Add student timeline entry")
	@PostMapping("/{id}/timeline")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF','TEACHER')")
	public ResponseEntity<ApiResponse<TimelineDTO>> addTimelineEntry(
			@PathVariable Long id,
			@Valid @RequestBody TimelineCreateRequest request) {
		TimelineDTO timelineDTO = new TimelineDTO();
		timelineDTO.setEventType(request.getEventType().name());
		timelineDTO.setTitle(request.getTitle());
		timelineDTO.setDescription(request.getDescription());
		return ResponseEntity.status(201)
				.body(ApiResponse.success("Timeline entry added", studentService.addTimelineEntry(id, timelineDTO)));
	}

	@io.swagger.v3.oas.annotations.Operation(summary = "Get student documents")
	@GetMapping("/{id}/documents")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF','TEACHER') or hasAuthority('STUDENT_ADMISSIONS_VIEW')")
	public ResponseEntity<ApiResponse<List<com.thinkerscave.student.dto.StudentDocumentDTO>>> getStudentDocuments(
			@PathVariable Long id) {
		log.info("Received request to get documents for student id: {}", id);
		return ResponseEntity.ok(ApiResponse.success(studentService.getStudentDocuments(id)));
	}

	@io.swagger.v3.oas.annotations.Operation(summary = "Download a student document")
	@GetMapping("/document/{docId}/download")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF','TEACHER') or hasAuthority('STUDENT_ADMISSIONS_VIEW')")
	public ResponseEntity<Resource> downloadDocument(@PathVariable Long docId) {
		log.info("Received request to download document id: {}", docId);
		Resource file = studentService.downloadDocument(docId);
		ContentDisposition contentDisposition = ContentDisposition.attachment()
				.filename(file.getFilename() != null ? file.getFilename() : "document").build();

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString()).body(file);
	}

	@Operation(summary = "Bulk import students from Excel")
	@PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF')")
	public ResponseEntity<ApiResponse<StudentImportJobResponse>> importStudents(@RequestPart("file") MultipartFile file) {
		StudentImportJobResponse response = studentExcelService.importStudents(file);
		return ResponseEntity.status(202).body(ApiResponse.success("Student import job submitted", response));
	}

	@Operation(summary = "Get student import job summary")
	@GetMapping("/import/{jobId}")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF')")
	public ResponseEntity<ApiResponse<com.thinkerscave.student.dto.BulkUploadResponse>> getImportSummary(
			@PathVariable String jobId) {
		return ResponseEntity.ok(ApiResponse.success("Import summary loaded", studentExcelService.getImportSummary(jobId)));
	}

	@Operation(summary = "Download student import validation report")
	@GetMapping("/import/{jobId}/errors")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF')")
	public ResponseEntity<Resource> downloadImportErrors(@PathVariable String jobId) {
		Resource resource = studentExcelService.downloadValidationReport(jobId);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student_import_errors_" + jobId + ".txt")
				.contentType(MediaType.TEXT_PLAIN)
				.body(resource);
	}

	@Operation(summary = "Read-only alumni directory")
	@GetMapping("/alumni")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF','TEACHER')")
	public ResponseEntity<ApiResponse<List<StudentResponseDTO>>> getAlumni(
			@RequestParam(required = false) String keyword) {
		StudentSearchRequest search = new StudentSearchRequest();
		search.setStatus(StudentStatus.ALUMNI);
		search.setKeyword(keyword);
		List<StudentResponseDTO> data = studentService.searchStudents(search, Pageable.unpaged()).getContent();
		return ResponseEntity.ok(ApiResponse.success("Alumni loaded", data));
	}

	@Operation(summary = "Alumni profile")
	@GetMapping("/alumni/{studentId}")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF','TEACHER')")
	public ResponseEntity<ApiResponse<com.thinkerscave.student.dto.StudentProfileResponse>> getAlumniProfile(
			@PathVariable Long studentId) {
		StudentResponseDTO student = studentService.getStudentById(studentId);
		if (!StudentStatus.ALUMNI.name().equals(student.getStatus())) {
			return ResponseEntity.badRequest().body(ApiResponse.error("Selected student is not an alumni"));
		}
		return ResponseEntity.ok(ApiResponse.success("Alumni profile loaded", studentService.getProfile360(studentId)));
	}

	@Operation(summary = "Master data: Academic years")
	@GetMapping("/academic-years")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<List<LookupDTO>>> getAcademicYears() {
		return ResponseEntity.ok(ApiResponse.success("Academic years loaded", academicsLookupService.getActiveAcademicYears()));
	}

	@Operation(summary = "Master data: Classes by academic year")
	@GetMapping("/classes")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<List<LookupDTO>>> getClasses(@RequestParam(required = false) Long academicYearId) {
		if (academicYearId == null) {
			List<LookupDTO> years = academicsLookupService.getActiveAcademicYears();
			if (years.isEmpty()) {
				return ResponseEntity.ok(ApiResponse.success("No academic year configured", List.of()));
			}
			academicYearId = years.get(0).getId();
		}
		return ResponseEntity.ok(ApiResponse.success("Classes loaded", academicsLookupService.getClassesByYear(academicYearId)));
	}

	@Operation(summary = "Master data: Sections by class")
	@GetMapping("/sections")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<List<LookupDTO>>> getSections(@RequestParam Long classId) {
		return ResponseEntity.ok(ApiResponse.success("Sections loaded", academicsLookupService.getSectionsByClass(classId)));
	}

	@Operation(summary = "Master data: Blood groups")
	@GetMapping("/blood-groups")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<List<String>>> getBloodGroups() {
		return ResponseEntity.ok(ApiResponse.success("Blood groups loaded",
				List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")));
	}

	@Operation(summary = "Master data: Document types")
	@GetMapping("/document-types")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<List<String>>> getDocumentTypes(@RequestParam(required = false) String query) {
		List<String> types = List.of("AADHAR", "BIRTH_CERTIFICATE", "TRANSFER_CERTIFICATE", "MARKSHEET", "CASTE_CERTIFICATE", "MEDICAL_RECORD", "PHOTO", "OTHER");
		if (!StringUtils.hasText(query)) {
			return ResponseEntity.ok(ApiResponse.success("Document types loaded", types));
		}
		String key = query.toLowerCase();
		return ResponseEntity.ok(ApiResponse.success("Document types loaded",
				types.stream().filter(t -> t.toLowerCase().contains(key)).collect(Collectors.toList())));
	}
}
