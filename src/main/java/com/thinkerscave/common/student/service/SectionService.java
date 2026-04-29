package com.thinkerscave.common.student.service;

import com.thinkerscave.common.student.dto.SectionDTO;

import java.util.List;

public interface SectionService {
	List<SectionDTO> getListOfSectionByClassId(Long classId);

	SectionDTO saveOrUpdate(SectionDTO sectionDTO);

	void delete(Long id);
}
