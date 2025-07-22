package com.thinkerscave.common.student.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.thinkerscave.common.student.domain.ClassEntity;
import com.thinkerscave.common.student.repository.ClassRepository;
import com.thinkerscave.common.student.service.ClassService;

@Service
public class ClassServiceImpl implements ClassService {

	@Autowired
	private ClassRepository classRepository;

	@Override
	public List<ClassEntity> getListOfClass() {

		try {
			List<ClassEntity> classes = classRepository.findAll();

			if (!classes.isEmpty()) {
				return classes;
			} else {
				return Collections.emptyList();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList();
		}

	}

}
