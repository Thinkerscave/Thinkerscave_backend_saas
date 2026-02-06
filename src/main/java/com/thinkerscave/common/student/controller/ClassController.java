package com.thinkerscave.common.student.controller;

import com.thinkerscave.common.student.domain.ClassEntity;
import com.thinkerscave.common.student.service.ClassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/class")
@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
public class ClassController {

	private final ClassService classService;

	@io.swagger.v3.oas.annotations.Operation(summary = "Get list of classes", parameters = {
			@io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
	})
	@GetMapping("/getListOfClass")
	public List<ClassEntity> getListOfClasses() {
		log.info("Fetching list of all classes");
		List<ClassEntity> classList = classService.getListOfClass();
		if (!classList.isEmpty()) {
			return classList;
		} else {
			return Collections.emptyList();
		}
	}

}
