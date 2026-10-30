package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TimetableConflict;
import com.thinkerscave.academics.enums.TimetableConflictStatus;
import com.thinkerscave.academics.enums.TimetableConflictType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TimetableConflictRepository extends JpaRepository<TimetableConflict, Long> {

    List<TimetableConflict> findByTimetableVersion_TimetableVersionId(Long versionId);

    List<TimetableConflict> findByTimetableVersion_TimetableVersionIdAndStatus(
            Long versionId, TimetableConflictStatus status);

    List<TimetableConflict> findByTimetableVersion_TimetableVersionIdAndBlockingTrueAndStatus(
            Long versionId, TimetableConflictStatus status);

    long countByTimetableVersion_TimetableVersionId(Long versionId);

    long countByTimetableVersion_TimetableVersionIdAndBlockingTrueAndStatus(
            Long versionId, TimetableConflictStatus status);

    long countByTimetableVersion_TimetableVersionIdAndBlockingFalseAndStatus(
            Long versionId, TimetableConflictStatus status);

    long countByTimetableVersion_TimetableVersionIdAndBlockingTrueAndStatusAndSection_AcademicGroup_AcademicGroupId(
            Long versionId, TimetableConflictStatus status, Long academicGroupId);

    @Query("""
            SELECT A FROM TimetableConflict A
            LEFT JOIN FETCH A.section s
            LEFT JOIN FETCH s.academicGroup
            LEFT JOIN FETCH A.timetablePeriod
            WHERE A.timetableVersion.timetableVersionId = :versionId
            ORDER BY A.blocking DESC, A.timetableConflictId ASC
            """)
    List<TimetableConflict> findDetailedByVersion(@Param("versionId") Long versionId);

    @Modifying
    @Query("DELETE FROM TimetableConflict A WHERE A.timetableVersion.timetableVersionId IN :versionIds")
    int deleteByVersionIds(@Param("versionIds") Collection<Long> versionIds);

    /** Manual validation recomputes section and teacher clashes, so stale rows must be replaced. */
    @Modifying
    @Query("""
            DELETE FROM TimetableConflict A
            WHERE A.timetableVersion.timetableVersionId = :versionId
              AND A.conflictType IN :types
            """)
    int deleteByVersionAndTypes(@Param("versionId") Long versionId,
                                @Param("types") Collection<TimetableConflictType> types);
}
