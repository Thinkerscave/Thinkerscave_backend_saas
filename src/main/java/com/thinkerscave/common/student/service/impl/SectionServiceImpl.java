package com.thinkerscave.common.student.service.impl;

import org.springframework.transaction.annotation.Transactional;
import com.thinkerscave.common.student.domain.Section;
import com.thinkerscave.common.student.repository.SectionRepository;
import com.thinkerscave.common.student.service.SectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.thinkerscave.common.student.domain.ClassEntity;
import com.thinkerscave.common.student.dto.SectionDTO;
import com.thinkerscave.common.student.repository.ClassRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class SectionServiceImpl implements SectionService {

	private final SectionRepository sectionRepository;
	private final ClassRepository classRepository;

	@Override
	public List<SectionDTO> getListOfSectionByClassId(Long classId) {
		List<Section> sections = sectionRepository.findByClassEntity_ClassId(classId);
		return sections.isEmpty() ? Collections.emptyList() : sections.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

	@Override
	@Transactional
	public SectionDTO saveOrUpdate(SectionDTO dto) {
		Section section;
		if (dto.getSectionId() != null) {
			section = sectionRepository.findById(dto.getSectionId())
					.orElseThrow(() -> new RuntimeException("Section not found"));
		} else {
			section = new Section();
		}

		section.setSectionName(dto.getSectionName());

		ClassEntity classEntity = classRepository.findById(dto.getClassId())
				.orElseThrow(() -> new RuntimeException("Class not found"));
		section.setClassEntity(classEntity);

		Section saved = sectionRepository.save(section);
		return mapToDTO(saved);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		sectionRepository.deleteById(id);
	}

	private SectionDTO mapToDTO(Section entity) {
		SectionDTO dto = new SectionDTO();
		dto.setSectionId(entity.getSectionId());
		dto.setSectionName(entity.getSectionName());
		if (entity.getClassEntity() != null) {
			dto.setClassId(entity.getClassEntity().getClassId());
		}
		return dto;
	}
}
