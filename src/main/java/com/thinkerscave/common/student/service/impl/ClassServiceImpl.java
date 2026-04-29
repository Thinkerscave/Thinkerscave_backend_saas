package com.thinkerscave.common.student.service.impl;

import org.springframework.transaction.annotation.Transactional;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.student.domain.ClassEntity;
import com.thinkerscave.common.student.repository.ClassRepository;
import com.thinkerscave.common.student.service.ClassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.thinkerscave.common.student.dto.ClassDTO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

	private final ClassRepository classRepository;

	@Override
	public List<ClassDTO> getListOfClass() {
		Long orgId = OrganizationContext.getOrganizationId();
		List<ClassEntity> classes = (orgId != null)
				? classRepository.findByOrganizationId(orgId)
				: classRepository.findAll();
		return classes.isEmpty() ? Collections.emptyList() : classes.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

	@Override
	@Transactional
	public ClassDTO saveOrUpdate(ClassDTO dto) {
		ClassEntity classEntity;
		if (dto.getClassId() != null) {
			classEntity = classRepository.findById(dto.getClassId())
					.orElseThrow(() -> new RuntimeException("Class not found"));
		} else {
			classEntity = new ClassEntity();
			Long orgId = OrganizationContext.getOrganizationId();
			if (orgId != null) {
				classEntity.setOrganizationId(orgId);
			}
		}
		classEntity.setClassName(dto.getClassName());
		ClassEntity saved = classRepository.save(classEntity);
		return mapToDTO(saved);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		classRepository.deleteById(id);
	}

	private ClassDTO mapToDTO(ClassEntity entity) {
		ClassDTO dto = new ClassDTO();
		dto.setClassId(entity.getClassId());
		dto.setClassName(entity.getClassName());
		return dto;
	}
}
