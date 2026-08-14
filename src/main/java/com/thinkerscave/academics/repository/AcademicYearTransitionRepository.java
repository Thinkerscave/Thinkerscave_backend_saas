package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.AcademicYearTransition;
import com.thinkerscave.academics.enums.AcademicTransitionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicYearTransitionRepository extends JpaRepository<AcademicYearTransition, Long> {

    Optional<AcademicYearTransition> findBySourceAcademicYear_AcademicYearIdAndTargetAcademicYear_AcademicYearId(
            Long sourceYearId, Long targetYearId);

    List<AcademicYearTransition> findByTargetAcademicYear_AcademicYearIdAndStatusIn(
            Long targetYearId, List<AcademicTransitionStatus> statuses);
}
