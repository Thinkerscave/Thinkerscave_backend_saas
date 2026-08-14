package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TimetableConfiguration;
import com.thinkerscave.academics.enums.TimetableConfigurationStatus;
import com.thinkerscave.academics.enums.TimetableShiftType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimetableConfigurationRepository extends JpaRepository<TimetableConfiguration, Long> {

    List<TimetableConfiguration> findByAcademicYear_AcademicYearId(Long academicYearId);

    Optional<TimetableConfiguration> findByAcademicYear_AcademicYearIdAndShiftType(
            Long academicYearId, TimetableShiftType shiftType);

    List<TimetableConfiguration> findByAcademicYear_AcademicYearIdAndStatus(
            Long academicYearId, TimetableConfigurationStatus status);
}
