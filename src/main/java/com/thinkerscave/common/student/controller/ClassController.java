package com.thinkerscave.common.student.controller;

import com.thinkerscave.common.student.domain.ClassEntity;
import com.thinkerscave.common.student.service.ClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/class")
@CrossOrigin("*")
public class ClassController {
	
	@Autowired
	private ClassService classService;
	
	
	@GetMapping("/getListOfClass")
	public List<ClassEntity> getListOfClasses(){
		List<ClassEntity> classList=classService.getListOfClass();
		if(!classList.isEmpty()) {
			System.out.println("in class if");
			return classList;
		}
		else {
			return Collections.emptyList();
		}
	}

}
