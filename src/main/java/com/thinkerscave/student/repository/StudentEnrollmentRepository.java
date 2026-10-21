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
			LEFT JOIN FETCH e.academicYear
			LEFT JOIN FETCH e.classEntity c
			LEFT JOIN FETCH c.academicYear
			LEFT JOIN FETCH e.section
			WHERE e.classEntity.classId = :classId
			  AND e.section.sectionId = :sectionId
			  AND e.active = true
			ORDER BY e.rollNumber ASC
			""")
	List<StudentEnrollment> findActiveRosterByClassSection(@Param("classId") Long classId,
			@Param("sectionId") Long sectionId);

	@Query("""
			SELECT e FROM StudentEnrollment e
			LEFT JOIN FETCH e.student
			LEFT JOIN FETCH e.academicYear
			LEFT JOIN FETCH e.classEntity c
			LEFT JOIN FETCH c.academicYear
			LEFT JOIN FETCH e.section
			WHERE e.classEntity.classId = :classId
			  AND e.active = true
			ORDER BY e.rollNumber ASC
			""")
	List<StudentEnrollment> findActiveRosterByClass(@Param("classId") Long classId);

	long countByClassEntityClassIdAndSectionSectionIdAndActiveTrue(Long classId, Long sectionId);

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

	@Query("""
			SELECT e FROM StudentEnrollment e
			JOIN FETCH e.student s
			LEFT JOIN FETCH e.classEntity c
			LEFT JOIN FETCH e.section sec
			WHERE e.active = true
			  AND (:classId IS NULL OR c.classId = :classId)
			  AND (:keyword IS NULL OR LOWER(s.firstName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
			       OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
			       OR LOWER(COALESCE(s.rollNumber, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
			       OR LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
			""")
	java.util.List<StudentEnrollment> searchFrontDeskDirectory(
			@org.springframework.data.repository.query.Param("classId") Long classId,
			@org.springframework.data.repository.query.Param("keyword") String keyword,
			org.springframework.data.domain.Pageable pageable);

}
