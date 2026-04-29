package com.thinkerscave.common.student.controller;

import com.thinkerscave.common.student.dto.ClassDTO;
import com.thinkerscave.common.student.service.ClassService;
import com.thinkerscave.common.commonModel.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/classes")
@Tag(name = "Class Management", description = "APIs for managing academic classes and grades")
@RequiredArgsConstructor
@Slf4j
public class ClassController {

	private final ClassService classService;

	@Operation(summary = "Get list of classes")
	@GetMapping("/getListOfClass")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN', 'STAFF', 'TEACHER') or hasAuthority('STUDENT_ADMISSIONS_VIEW')")
	public ResponseEntity<ApiResponse<List<ClassDTO>>> getListOfClasses() {
		log.info("Fetching list of all classes");
		List<ClassDTO> classList = classService.getListOfClass();
		return ResponseEntity.ok(ApiResponse.success("Classes fetched successfully", classList));
	}

	@Operation(summary = "Create or Update Class")
	@PostMapping("/saveOrUpdate")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
	public ResponseEntity<ApiResponse<ClassDTO>> saveOrUpdate(@Valid @RequestBody ClassDTO classDTO) {
		log.info("Saving/updating class: {}", classDTO.getClassName());
		ClassDTO savedClass = classService.saveOrUpdate(classDTO);
		return ResponseEntity.ok(ApiResponse.success("Class saved successfully", savedClass));
	}

	@Operation(summary = "Delete Class")
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
		log.info("Deleting class id: {}", id);
		classService.delete(id);
		return ResponseEntity.ok(ApiResponse.success("Class deleted successfully", null));
	}
}
