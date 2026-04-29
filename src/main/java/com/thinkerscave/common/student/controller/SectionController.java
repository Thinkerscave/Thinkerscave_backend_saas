package com.thinkerscave.common.student.controller;

import com.thinkerscave.common.student.dto.SectionDTO;
import com.thinkerscave.common.student.service.SectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.thinkerscave.common.commonModel.ApiResponse;

import java.util.Collections;
import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/sections")
@Tag(name = "Section Management", description = "APIs for managing class sections/divisions")
@RequiredArgsConstructor
@Slf4j
public class SectionController {

	private final SectionService sectionService;

	@Operation(summary = "Get sections by class ID")
	@GetMapping("getListOfSectionsByClassId/{classId}")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN', 'STAFF', 'TEACHER') or hasAuthority('STUDENT_ADMISSIONS_VIEW')")
	public ResponseEntity<ApiResponse<List<SectionDTO>>> getListOfSectionsByClassId(@PathVariable("classId") Long classId) {
		log.info("Fetching sections for classId: {}", classId);
		List<SectionDTO> sectionList = sectionService.getListOfSectionByClassId(classId);
		return ResponseEntity.ok(ApiResponse.success("Sections fetched successfully", sectionList));
	}

	@Operation(summary = "Create or Update Section")
	@PostMapping("/saveOrUpdate")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
	public ResponseEntity<ApiResponse<SectionDTO>> saveOrUpdate(@Valid @RequestBody SectionDTO sectionDTO) {
		log.info("Saving/updating section: {}", sectionDTO.getSectionName());
		SectionDTO savedSection = sectionService.saveOrUpdate(sectionDTO);
		return ResponseEntity.ok(ApiResponse.success("Section saved successfully", savedSection));
	}

	@Operation(summary = "Delete Section")
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
		log.info("Deleting section id: {}", id);
		sectionService.delete(id);
		return ResponseEntity.ok(ApiResponse.success("Section deleted successfully", null));
	}
}
