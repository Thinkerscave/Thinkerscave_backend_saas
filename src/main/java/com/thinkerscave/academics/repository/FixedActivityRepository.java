package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.FixedActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FixedActivityRepository extends JpaRepository<FixedActivity, Long> {

    @Query("""
            SELECT DISTINCT a FROM FixedActivity a
            LEFT JOIN FETCH a.days
            LEFT JOIN FETCH a.groups g
            LEFT JOIN FETCH g.academicGroup
            LEFT JOIN FETCH g.section
            WHERE a.academicYear.academicYearId = :yearId
            ORDER BY a.name ASC
            """)
    List<FixedActivity> findDetailedByYear(@Param("yearId") Long yearId);

    @Query("""
            SELECT DISTINCT a FROM FixedActivity a
            LEFT JOIN FETCH a.days
            LEFT JOIN FETCH a.groups g
            LEFT JOIN FETCH g.academicGroup
            LEFT JOIN FETCH g.section
            WHERE a.fixedActivityId = :id
            """)
    Optional<FixedActivity> findDetailedById(@Param("id") Long id);
}
