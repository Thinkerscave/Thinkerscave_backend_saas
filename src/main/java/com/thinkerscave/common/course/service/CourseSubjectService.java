package com.thinkerscave.common.course.service;

import com.thinkerscave.common.course.dto.CourseRequestDTO;
import com.thinkerscave.common.course.dto.CourseResponseDTO;
import com.thinkerscave.common.course.dto.SubjectRequestDTO;
import com.thinkerscave.common.course.dto.SubjectResponseDTO;

import java.util.List;

/**
 * Service for managing Courses and Subjects.
 */
public interface CourseSubjectService {

    // Course Management
    CourseResponseDTO createCourse(CourseRequestDTO dto);

    CourseResponseDTO updateCourse(Long courseId, CourseRequestDTO dto);

    CourseResponseDTO getCourse(Long courseId);

    List<CourseResponseDTO> getAllCoursesByOrg(Long orgId);

    void deleteCourse(Long courseId);

    // Subject Management
    SubjectResponseDTO createSubject(SubjectRequestDTO dto);

    SubjectResponseDTO updateSubject(Long subjectId, SubjectRequestDTO dto);

    SubjectResponseDTO getSubject(Long subjectId);

    List<SubjectResponseDTO> getAllSubjectsByOrg(Long orgId);

    void deleteSubject(Long subjectId);
}
