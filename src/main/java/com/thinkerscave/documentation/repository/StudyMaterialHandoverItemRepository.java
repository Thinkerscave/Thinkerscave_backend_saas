package com.thinkerscave.documentation.repository;

import com.thinkerscave.documentation.entity.StudyMaterialHandoverItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyMaterialHandoverItemRepository extends JpaRepository<StudyMaterialHandoverItem, Long> {
    List<StudyMaterialHandoverItem> findByHandoverId(Long handoverId);
}
