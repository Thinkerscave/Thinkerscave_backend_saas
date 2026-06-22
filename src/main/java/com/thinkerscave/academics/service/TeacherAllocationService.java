package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.ClassTeacherAssignmentRequest;
import com.thinkerscave.academics.dto.request.SubjectAssignmentRequest;
import com.thinkerscave.academics.dto.response.ClassTeacherAssignmentResponse;
import com.thinkerscave.academics.dto.response.SubjectAssignmentResponse;
import com.thinkerscave.academics.dto.response.TeacherWorkloadResponse;

import java.util.List;

public interface TeacherAllocationService {

    // Class teacher
    ClassTeacherAssignmentResponse assignClassTeacher(ClassTeacherAssignmentRequest request);

    ClassTeacherAssignmentResponse getClassTeacherAssignment(Long assignmentId);

    List<ClassTeacherAssignmentResponse> getClassTeacherAssignments(Long yearId, Long classId, Long sectionId);

    void removeClassTeacher(Long assignmentId);

    // Subject assignment
    SubjectAssignmentResponse assignSubject(SubjectAssignmentRequest request);

    SubjectAssignmentResponse updateSubjectAssignment(Long assignmentId, SubjectAssignmentRequest request);

    List<SubjectAssignmentResponse> getSubjectAssignments(Long yearId, Long classId, Long sectionId);

    void removeSubjectAssignment(Long assignmentId);

    // Workload
    TeacherWorkloadResponse getTeacherWorkload(Long teacherId, Long academicYearId);
}
