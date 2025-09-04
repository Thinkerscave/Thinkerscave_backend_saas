package com.thinkerscave.common.student.repository;


import com.thinkerscave.common.student.domain.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section,Long> {
	
	List<Section> findByClassEntity_ClassId(Long classId);
}
