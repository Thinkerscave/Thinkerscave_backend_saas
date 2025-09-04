package com.thinkerscave.common.student.service;

import com.thinkerscave.common.student.domain.Section;

import java.util.List;

public interface SectionService {
	
	public List<Section> getListOfSectionByClassId(Long classId);

}
