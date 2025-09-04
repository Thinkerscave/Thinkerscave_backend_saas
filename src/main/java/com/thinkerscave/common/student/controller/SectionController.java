package com.thinkerscave.common.student.controller;

import com.thinkerscave.common.student.domain.Section;
import com.thinkerscave.common.student.service.SectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/section")
@CrossOrigin("*")
public class SectionController {


	@Autowired
	private SectionService sectionService;
	

	@GetMapping("getListOfSectionsByClassId/{classId}")
	public List<Section> getListOfSectionsByClassId(@PathVariable("classId")Long classId){
		List<Section> sectionList=sectionService.getListOfSectionByClassId(classId);
		if(!sectionList.isEmpty()) {
			return sectionList;
		}
		else {
			return Collections.emptyList();
		}
	}

	@GetMapping("/hello")
	public String ex(){
		return "hello";
	}

}
