package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TimetableVersion;
import com.thinkerscave.academics.enums.TimetableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimetableVersionRepository extends JpaRepository<TimetableVersion, Long> {

    List<TimetableVersion> findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(Long academicYearId);

    Optional<TimetableVersion> findByAcademicYear_AcademicYearIdAndStatus(Long academicYearId, TimetableStatus status);

    Optional<TimetableVersion> findByAcademicYear_AcademicYearIdAndVersionNumber(Long academicYearId, Integer versionNumber);

    @Query("""
            SELECT v FROM TimetableVersion v
            JOIN FETCH v.timetableConfiguration
            JOIN FETCH v.academicYear
            WHERE v.timetableVersionId = :id
            """)
    Optional<TimetableVersion> findByIdWithConfigAndYear(@Param("id") Long id);
}
