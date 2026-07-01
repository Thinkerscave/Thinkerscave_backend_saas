package com.thinkerscave.student.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thinkerscave.student.entity.StudentEnrollment;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {

	Optional<StudentEnrollment> findByStudentStudentIdAndActiveTrue(Long studentId);

	List<StudentEnrollment> findByStudentStudentIdOrderByEnrollmentIdDesc(Long studentId);

	List<StudentEnrollment> findByClassEntityClassIdAndActiveTrueOrderByStudentFirstNameAsc(Long classId);

	List<StudentEnrollment> findByClassEntityClassIdAndSectionSectionIdAndActiveTrueOrderByRollNumber(
			Long classId, Long sectionId);
}
