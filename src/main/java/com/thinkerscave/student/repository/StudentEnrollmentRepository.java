package com.thinkerscave.student.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.thinkerscave.student.entity.StudentEnrollment;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {

	Optional<StudentEnrollment> findByStudentStudentIdAndActiveTrue(Long studentId);

	@Query("""
			SELECT e FROM StudentEnrollment e
			LEFT JOIN FETCH e.academicYear
			LEFT JOIN FETCH e.classEntity c
			LEFT JOIN FETCH c.academicYear
			LEFT JOIN FETCH e.section
			WHERE e.student.studentId = :studentId AND e.active = true
			""")
	Optional<StudentEnrollment> findActiveWithClassByStudentId(@Param("studentId") Long studentId);

	/** Eagerly fetches class + section + year to avoid LazyInitializationException. */
	@Query("""
			SELECT e FROM StudentEnrollment e
			LEFT JOIN FETCH e.academicYear
			LEFT JOIN FETCH e.academicGroupEntity A
			LEFT JOIN FETCH A.academicYear
			LEFT JOIN FETCH e.section
			WHERE e.student.studentId = :studentId AND e.active = true
			""")
	Optional<StudentEnrollment> findActiveWithAcademicGroupByStudentId(@Param("studentId") Long studentId);

	@Query("""
			SELECT e FROM StudentEnrollment e
			LEFT JOIN FETCH e.academicYear
			LEFT JOIN FETCH e.academicGroupEntity
			LEFT JOIN FETCH e.classEntity
			LEFT JOIN FETCH e.section
			WHERE e.student.studentId = :studentId
			ORDER BY e.enrollmentId DESC
			""")
	List<StudentEnrollment> findHistoryWithDetailsByStudentId(@Param("studentId") Long studentId);

	List<StudentEnrollment> findByAcademicGroupEntityAcademicGroupIdAndActiveTrueOrderByStudentFirstNameAsc(Long academicGroupId);

	List<StudentEnrollment> findByClassEntityClassIdAndActiveTrueOrderByStudentFirstNameAsc(Long classId);

	boolean existsByAcademicGroupEntityAcademicGroupIdAndActiveTrue(Long academicGroupId);

	boolean existsByClassEntityClassIdAndActiveTrue(Long classId);

	List<StudentEnrollment> findByAcademicGroupEntityAcademicGroupIdAndSectionSectionIdAndActiveTrueOrderByRollNumber(
			Long academicGroupId, Long sectionId);

	List<StudentEnrollment> findByClassEntityClassIdAndSectionSectionIdAndActiveTrueOrderByRollNumber(
			Long classId, Long sectionId);

	@Query("""
			SELECT e FROM StudentEnrollment e
			LEFT JOIN FETCH e.student
			LEFT JOIN FETCH e.academicYear
			LEFT JOIN FETCH e.academicGroupEntity A
			LEFT JOIN FETCH A.academicYear
			LEFT JOIN FETCH e.section
			WHERE e.academicGroupEntity.academicGroupId = :academicGroupId
			  AND e.section.sectionId = :sectionId
			  AND e.active = true
			ORDER BY e.rollNumber ASC
			""")
	List<StudentEnrollment> findActiveRosterByAcademicGroupSection(@Param("academicGroupId") Long academicGroupId,
			@Param("sectionId") Long sectionId);

	@Query("""
			SELECT e FROM StudentEnrollment e
			LEFT JOIN FETCH e.student
			LEFT JOIN FETCH e.academicYear
			LEFT JOIN FETCH e.classEntity c
			LEFT JOIN FETCH c.academicYear
			LEFT JOIN FETCH e.section
			WHERE e.classEntity.classId = :classId AND e.section.sectionId = :sectionId AND e.active = true
			ORDER BY e.rollNumber ASC
			""")
	List<StudentEnrollment> findActiveRosterByClassSection(@Param("classId") Long classId,
			@Param("sectionId") Long sectionId);

	@Query("""
			SELECT e FROM StudentEnrollment e
			LEFT JOIN FETCH e.student
			LEFT JOIN FETCH e.academicYear
			LEFT JOIN FETCH e.academicGroupEntity A
			LEFT JOIN FETCH A.academicYear
			LEFT JOIN FETCH e.section
			WHERE e.academicGroupEntity.academicGroupId = :academicGroupId
			  AND e.active = true
			ORDER BY e.rollNumber ASC
			""")
	List<StudentEnrollment> findActiveRosterByAcademicGroup(@Param("academicGroupId") Long academicGroupId);

	@Query("""
			SELECT e FROM StudentEnrollment e
			LEFT JOIN FETCH e.student
			LEFT JOIN FETCH e.academicYear
			LEFT JOIN FETCH e.classEntity c
			LEFT JOIN FETCH c.academicYear
			LEFT JOIN FETCH e.section
			WHERE e.classEntity.classId = :classId AND e.active = true
			ORDER BY e.rollNumber ASC
			""")
	List<StudentEnrollment> findActiveRosterByClass(@Param("classId") Long classId);

	long countByAcademicGroupEntityAcademicGroupIdAndSectionSectionIdAndActiveTrue(Long academicGroupId, Long sectionId);

	long countByClassEntityClassIdAndSectionSectionIdAndActiveTrue(Long classId, Long sectionId);

	@Query("""
			SELECT e FROM StudentEnrollment e
			LEFT JOIN FETCH e.student
			LEFT JOIN FETCH e.academicGroupEntity
			LEFT JOIN FETCH e.classEntity
			LEFT JOIN FETCH e.section
			LEFT JOIN FETCH e.academicYear
			WHERE e.academicYear.academicYearId = :yearId
			  AND e.active = true
			  AND e.status = com.thinkerscave.student.enums.EnrollmentStatus.ACTIVE
			ORDER BY e.student.firstName ASC
			""")
	List<StudentEnrollment> findActiveByAcademicYearId(@Param("yearId") Long yearId);

	boolean existsByStudentStudentIdAndAcademicYearAcademicYearId(Long studentId, Long academicYearId);

	long countByAcademicGroupEntityAcademicGroupIdAndActiveTrue(Long academicGroupId);

	long countByClassEntityClassIdAndActiveTrue(Long classId);

	long countBySectionSectionIdAndActiveTrue(Long sectionId);

	long countByAcademicYearAcademicYearIdAndActiveTrue(Long academicYearId);

	/** One query for all group student counts in a year: rows of [academicGroupId, count]. */
	@Query("""
			SELECT e.academicGroupEntity.academicGroupId, Count(e) FROM StudentEnrollment e
			WHERE e.academicGroupEntity.academicYear.academicYearId = :yearId AND e.active = true
			GROUP BY e.academicGroupEntity.academicGroupId
			""")
	List<Object[]> countActiveByAcademicGroupForYear(@Param("yearId") Long yearId);
}
