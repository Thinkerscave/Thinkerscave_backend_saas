package com.thinkerscave.student.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thinkerscave.student.entity.StudentParent;

@Repository
public interface StudentParentRepository extends JpaRepository<StudentParent, Long> {

	List<StudentParent> findByStudentStudentId(Long studentId);

	/** Used by the Parent dashboard to resolve linked children (list-based, future-ready for multiple children). */
	List<StudentParent> findByParent_ParentIdAndActiveTrue(Long parentId);
}
