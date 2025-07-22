package com.thinkerscave.common.student.repository;


import com.thinkerscave.common.student.domain.Section;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionRepository extends JpaRepository<Section,Long> {
	
	List<Section> findByClassEntity_ClassId(Long classId);
}
