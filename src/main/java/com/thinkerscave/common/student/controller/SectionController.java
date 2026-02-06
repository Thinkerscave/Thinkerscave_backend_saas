package com.thinkerscave.common.student.controller;

import com.thinkerscave.common.student.domain.Section;
import com.thinkerscave.common.student.service.SectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/section")
@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
public class SectionController {

	private final SectionService sectionService;

	@io.swagger.v3.oas.annotations.Operation(summary = "Get sections by class ID", parameters = {
			@io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
	})
	@GetMapping("getListOfSectionsByClassId/{classId}")
	public List<Section> getListOfSectionsByClassId(@PathVariable("classId") Long classId) {
		log.info("Fetching sections for classId: {}", classId);
		List<Section> sectionList = sectionService.getListOfSectionByClassId(classId);
		if (!sectionList.isEmpty()) {
			return sectionList;
		} else {
			return Collections.emptyList();
		}
	}

	@GetMapping("/hello")
	public String ex() {
		return "hello";
	}

}
