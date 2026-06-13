package com.thinkerscave.student.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.thinkerscave.student.dto.StudentCreateRequest;
import com.thinkerscave.student.dto.StudentResponseDTO;

public interface StudentService {

	StudentResponseDTO createStudent(StudentCreateRequest request);

	StudentResponseDTO updateStudent(Long studentId, StudentCreateRequest request);

	StudentResponseDTO getStudentById(Long studentId);

	Page<StudentResponseDTO> getStudents(Pageable pageable);

	void deleteStudent(Long studentId);

}
