package com.thinkerscave.common.student.service;

import com.thinkerscave.common.student.dto.ClassDTO;

import java.util.List;

public interface ClassService {
	List<ClassDTO> getListOfClass();

	ClassDTO saveOrUpdate(ClassDTO dto);

	void delete(Long id);
}
