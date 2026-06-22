package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.AcademicSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicScheduleRepository extends JpaRepository<AcademicSchedule, Long> {

    List<AcademicSchedule> findByAcademicYear_AcademicYearIdAndActiveOrderByStartDateAsc(Long yearId, Boolean active);

    List<AcademicSchedule> findByAcademicYear_AcademicYearIdOrderByStartDateAsc(Long yearId);
}
