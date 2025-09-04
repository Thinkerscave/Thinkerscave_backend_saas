package com.thinkerscave.common.student.service.impl;

import com.thinkerscave.common.student.domain.Section;
import com.thinkerscave.common.student.repository.SectionRepository;
import com.thinkerscave.common.student.service.SectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class SectionServiceImpl implements SectionService{
	
	@Autowired
	private SectionRepository sectionRepository;

	@Override
	public List<Section> getListOfSectionByClassId(Long classId) {
		try {
			 List<Section> sections = sectionRepository.findByClassEntity_ClassId(classId);

			if (!sections.isEmpty()) {
				return sections;
			} else {
				return Collections.emptyList();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
		
		
	}

}
