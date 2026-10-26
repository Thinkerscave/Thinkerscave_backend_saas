package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.GroupScheduleAssignment;
import com.thinkerscave.academics.enums.DayOfWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GroupScheduleAssignmentRepository extends JpaRepository<GroupScheduleAssignment, Long> {

    @Query("""
            SELECT a FROM GroupScheduleAssignment a
            JOIN FETCH a.academicGroup g
            LEFT JOIN FETCH a.section s
            JOIN FETCH a.schedulePattern p
            WHERE a.academicYear.academicYearId = :yearId
            ORDER BY g.displayOrder ASC, s.displayOrder ASC, a.dayOfWeek ASC, p.name ASC
            """)
    List<GroupScheduleAssignment> findDetailedByYear(@Param("yearId") Long yearId);

    List<GroupScheduleAssignment> findByAcademicYear_AcademicYearIdAndActiveTrue(Long yearId);

    List<GroupScheduleAssignment> findBySchedulePattern_SchedulePatternId(Long patternId);

    @Query("""
            SELECT COUNT(a) FROM GroupScheduleAssignment a
            WHERE a.schedulePattern.schedulePatternId = :patternId AND a.active = true
            """)
    long countActiveByPattern(@Param("patternId") Long patternId);

    @Query("""
            SELECT a FROM GroupScheduleAssignment a
            JOIN FETCH a.academicGroup g
            LEFT JOIN FETCH a.section s
            JOIN FETCH a.schedulePattern p
            WHERE a.academicYear.academicYearId = :yearId
              AND a.active = true
              AND a.dayOfWeek = :day
            """)
    List<GroupScheduleAssignment> findActiveByYearAndDay(
            @Param("yearId") Long yearId, @Param("day") DayOfWeek day);

    @Modifying
    @Query("""
            DELETE FROM GroupScheduleAssignment a
            WHERE a.academicGroup.academicGroupId IN :groupIds
              AND a.dayOfWeek = :day
            """)
    int deleteForGroupsOnDay(@Param("groupIds") Collection<Long> groupIds, @Param("day") DayOfWeek day);
}
