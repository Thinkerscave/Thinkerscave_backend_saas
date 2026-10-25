package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TimetableEntry;
import com.thinkerscave.academics.enums.DayOfWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

    List<TimetableEntry> findByTimetableVersion_TimetableVersionId(Long versionId);

    List<TimetableEntry> findByTimetableVersion_TimetableVersionIdAndSection_SectionId(
            Long versionId, Long sectionId);

    List<TimetableEntry> findByTimetableVersion_TimetableVersionIdAndDayOfWeek(
            Long versionId, DayOfWeek dayOfWeek);

    long countByTimetableVersion_TimetableVersionId(Long versionId);

    long countByTeacherAllocation_TeacherAllocationId(Long teacherAllocationId);

    @Query("""
            SELECT e FROM TimetableEntry e
            JOIN FETCH e.timetablePeriod p
            LEFT JOIN FETCH e.section s
            LEFT JOIN FETCH s.academicGroup
            LEFT JOIN FETCH e.teacherAllocation
            LEFT JOIN FETCH e.resource
            WHERE e.timetableVersion.timetableVersionId = :versionId
            """)
    List<TimetableEntry> findGridEntries(@Param("versionId") Long versionId);

    @Modifying
    @Query("DELETE FROM TimetableEntry e WHERE e.timetableVersion.timetableVersionId IN :versionIds")
    int deleteByVersionIds(@Param("versionIds") Collection<Long> versionIds);
}
