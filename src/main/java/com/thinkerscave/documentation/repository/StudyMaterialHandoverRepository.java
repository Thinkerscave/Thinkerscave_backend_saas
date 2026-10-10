package com.thinkerscave.documentation.repository;

import com.thinkerscave.documentation.entity.StudyMaterialHandover;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyMaterialHandoverRepository extends JpaRepository<StudyMaterialHandover, Long> {
    Page<StudyMaterialHandover> findByOrganizationId(Long organizationId, Pageable pageable);
}
