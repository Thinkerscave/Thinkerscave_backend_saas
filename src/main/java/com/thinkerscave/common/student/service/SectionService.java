package com.thinkerscave.common.student.service;

import java.util.List;

import com.thinkerscave.common.student.domain.Section;

public interface SectionService {
	
	public List<Section> getListOfSectionByClassId(Long classId);

}
