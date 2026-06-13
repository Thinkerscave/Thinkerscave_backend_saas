package com.thinkerscave.student.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thinkerscave.student.entity.StudentTimeline;

@Repository
public interface StudentTimelineRepository extends JpaRepository<StudentTimeline, Long> {

	List<StudentTimeline> findByStudentStudentIdOrderByCreatedDateDesc(Long studentId);
}
