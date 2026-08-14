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

	/** Eagerly fetches class + section + year to avoid LazyInitializationException. */
	@Query("""
			SELECT e FROM StudentEnrollment e
			LEFT JOIN FETCH e.academicYear
			LEFT JOIN FETCH e.classEntity c
			LEFT JOIN FETCH c.academicYear
			LEFT JOIN FETCH e.section
			WHERE e.student.studentId = :studentId AND e.active = true
			""")
	Optional<StudentEnrollment> findActiveWithClassByStudentId(@Param("studentId") Long studentId);

	@Query("""
			SELECT e FROM StudentEnrollment e
			LEFT JOIN FETCH e.academicYear
			LEFT JOIN FETCH e.classEntity
			LEFT JOIN FETCH e.section
			WHERE e.student.studentId = :studentId
			ORDER BY e.enrollmentId DESC
			""")
	List<StudentEnrollment> findHistoryWithDetailsByStudentId(@Param("studentId") Long studentId);

	List<StudentEnrollment> findByClassEntityClassIdAndActiveTrueOrderByStudentFirstNameAsc(Long classId);

	boolean existsByClassEntityClassIdAndActiveTrue(Long classId);

	List<StudentEnrollment> findByClassEntityClassIdAndSectionSectionIdAndActiveTrueOrderByRollNumber(
			Long classId, Long sectionId);

	@Query("""
			SELECT e FROM StudentEnrollment e
			LEFT JOIN FETCH e.student
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

	long countByClassEntityClassIdAndActiveTrue(Long classId);

	long countBySectionSectionIdAndActiveTrue(Long sectionId);

	long countByAcademicYearAcademicYearIdAndActiveTrue(Long academicYearId);
}
