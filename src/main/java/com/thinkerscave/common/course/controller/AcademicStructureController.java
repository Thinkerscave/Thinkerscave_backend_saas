
package com.thinkerscave.common.course.controller;

import com.thinkerscave.common.course.dto.AcademicContainerDTO;
import com.thinkerscave.common.course.dto.AcademicYearDTO;
import com.thinkerscave.common.course.dto.StructureTemplateDTO;
import com.thinkerscave.common.course.service.AcademicStructureService;
import com.thinkerscave.common.commonModel.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/academic-structure")
@Tag(name = "Academic Structure Management", description = "Professional APIs for institutional hierarchy, academic calendar, and bulk structural generation")
@RequiredArgsConstructor
public class AcademicStructureController {

	private final AcademicStructureService structureService;

	@PostMapping("/years")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('ACADEMIC_STRUCTURE_ADD')")
	@Operation(summary = "Initialize a new academic year")
	public ResponseEntity<ApiResponse<AcademicYearDTO>> createAcademicYear(
			@RequestParam Long orgId,
			@RequestParam String yearCode,
			@RequestParam String startDate,
			@RequestParam String endDate) {
		log.info("API Request - Create Academic Year: {} for Org: {}", yearCode, orgId);
		AcademicYearDTO created = structureService.createAcademicYear(orgId, yearCode, startDate, endDate);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Academic year created successfully", created));
	}

	@GetMapping("/years/{orgId}")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('ACADEMIC_STRUCTURE_VIEW')")
	@Operation(summary = "Fetch institutional calendar history")
	public ResponseEntity<ApiResponse<List<AcademicYearDTO>>> getAcademicYears(@PathVariable Long orgId) {
		log.info("API Request - Get All Academic Years for Org: {}", orgId);
		List<AcademicYearDTO> years = structureService.getAcademicYears(orgId);
		return ResponseEntity.ok(ApiResponse.success("Academic years retrieved successfully", years));
	}

	@GetMapping("/years/{orgId}/current")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('ACADEMIC_STRUCTURE_VIEW')")
	@Operation(summary = "Identify currently active session")
	public ResponseEntity<ApiResponse<AcademicYearDTO>> getCurrentYear(@PathVariable Long orgId) {
		log.info("API Request - Get Current Academic Year for Org: {}", orgId);
		AcademicYearDTO current = structureService.getCurrentAcademicYear(orgId);
		return ResponseEntity.ok(ApiResponse.success("Current academic year retrieved successfully", current));
	}

	@PostMapping("/years/{orgId}/current/{yearId}")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('ACADEMIC_STRUCTURE_EDIT')")
	@Operation(summary = "Switch the active academic session")
	public ResponseEntity<ApiResponse<Void>> setCurrentYear(@PathVariable Long orgId, @PathVariable Long yearId) {
		log.info("API Request - Switch Current Year to: {} for Org: {}", yearId, orgId);
		structureService.setCurrentAcademicYear(orgId, yearId);
		return ResponseEntity.ok(ApiResponse.success("Current academic year updated successfully", null));
	}

	@PostMapping("/containers")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('ACADEMIC_STRUCTURE_ADD')")
	@Operation(summary = "Add a new structural node")
	public ResponseEntity<ApiResponse<AcademicContainerDTO>> createContainer(@Valid @RequestBody AcademicContainerDTO dto) {
		log.info("API Request - Create Container: {} of Type: {}", dto.getContainerName(), dto.getContainerType());
		AcademicContainerDTO created = structureService.createContainer(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Structural container created successfully", created));
	}

	@PutMapping("/containers/{containerId}")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('ACADEMIC_STRUCTURE_EDIT')")
	@Operation(summary = "Update structural node parameters")
	public ResponseEntity<ApiResponse<AcademicContainerDTO>> updateContainer(@PathVariable Long containerId,
			@Valid @RequestBody AcademicContainerDTO dto) {
		log.info("API Request - Update Container ID: {}", containerId);
		AcademicContainerDTO updated = structureService.updateContainer(containerId, dto);
		return ResponseEntity.ok(ApiResponse.success("Structural container updated successfully", updated));
	}

	@GetMapping("/containers/{containerId}")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('ACADEMIC_STRUCTURE_VIEW')")
	@Operation(summary = "Fetch container profile")
	public ResponseEntity<ApiResponse<AcademicContainerDTO>> getContainer(@PathVariable Long containerId) {
		log.info("API Request - Get Container Profile: {}", containerId);
		AcademicContainerDTO container = structureService.getContainer(containerId);
		return ResponseEntity.ok(ApiResponse.success("Container profile retrieved successfully", container));
	}

	@GetMapping("/containers/org/{orgId}/year/{yearId}")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('ACADEMIC_STRUCTURE_VIEW')")
	@Operation(summary = "List root structural nodes")
	public ResponseEntity<ApiResponse<List<AcademicContainerDTO>>> getTopLevelContainers(
			@PathVariable Long orgId,
			@PathVariable Long yearId) {
		log.info("API Request - Get Top Level Containers for Org: {}, Year: {}", orgId, yearId);
		List<AcademicContainerDTO> containers = structureService.getTopLevelContainers(orgId, yearId);
		return ResponseEntity.ok(ApiResponse.success("Top-level containers retrieved successfully", containers));
	}

	@GetMapping("/containers/{parentId}/children")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('ACADEMIC_STRUCTURE_VIEW')")
	@Operation(summary = "Drill-down into sub-containers")
	public ResponseEntity<ApiResponse<List<AcademicContainerDTO>>> getChildContainers(@PathVariable Long parentId) {
		log.info("API Request - Get Child Containers for Parent: {}", parentId);
		List<AcademicContainerDTO> children = structureService.getChildContainers(parentId);
		return ResponseEntity.ok(ApiResponse.success("Child containers retrieved successfully", children));
	}

	@PostMapping("/generate-school")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('ACADEMIC_STRUCTURE_ADD')")
	@Operation(summary = "Batch-generate K-12 school structure")
	public ResponseEntity<ApiResponse<Void>> generateSchoolStructure(@RequestParam Long orgId, @RequestParam Long yearId) {
		log.info("API Request - Generate School Structure for Org: {}", orgId);
		structureService.generateSchoolStructure(orgId, yearId);
		return ResponseEntity.ok(ApiResponse.success("School structure generated successfully", null));
	}

	@PostMapping("/generate-college")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('ACADEMIC_STRUCTURE_ADD')")
	@Operation(summary = "Batch-generate higher-ed branch structure")
	public ResponseEntity<ApiResponse<Void>> generateCollegeStructure(
			@RequestParam Long orgId,
			@RequestParam Long yearId,
			@RequestParam Long courseId) {
		log.info("API Request - Generate College Structure for Org: {}, Course: {}", orgId, courseId);
		structureService.generateCollegeStructure(orgId, yearId, courseId);
		return ResponseEntity.ok(ApiResponse.success("College structure generated successfully", null));
	}

	@PostMapping("/generate-dynamic")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('ACADEMIC_STRUCTURE_ADD')")
	@Operation(summary = "Generate dynamic structure")
	public ResponseEntity<ApiResponse<Void>> generateDynamicStructure(
			@RequestParam Long orgId,
			@RequestParam Long yearId,
			@Valid @RequestBody StructureTemplateDTO template) {
		log.info("API Request - Generate Dynamic Structure for Org: {}", orgId);
		structureService.generateDynamicStructure(orgId, yearId, template);
		return ResponseEntity.ok(ApiResponse.success("Dynamic structure generated successfully", null));
	}

	@DeleteMapping("/containers/{containerId}")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('ACADEMIC_STRUCTURE_DELETE')")
	@Operation(summary = "Retire a structural node")
	public ResponseEntity<ApiResponse<Void>> deleteContainer(@PathVariable Long containerId) {
		log.info("API Request - Delete Container ID: {}", containerId);
		structureService.deleteContainer(containerId);
		return ResponseEntity.ok(ApiResponse.success("Container deleted successfully", null));
	}
}
