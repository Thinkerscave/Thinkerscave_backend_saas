package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.ClassTeacherAssignmentRequest;
import com.thinkerscave.academics.dto.request.SubjectAssignmentRequest;
import com.thinkerscave.academics.dto.response.ClassTeacherAssignmentResponse;
import com.thinkerscave.academics.dto.response.SubjectAssignmentResponse;
import com.thinkerscave.academics.dto.response.TeacherWorkloadResponse;
import com.thinkerscave.academics.service.TeacherAllocationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeacherAllocationServiceImpl implements TeacherAllocationService {

    private static final String MSG = "Academics teacher allocation API rebuild in progress";

    @Override
    public ClassTeacherAssignmentResponse assignClassTeacher(ClassTeacherAssignmentRequest request) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public ClassTeacherAssignmentResponse getClassTeacherAssignment(Long assignmentId) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public List<ClassTeacherAssignmentResponse> getClassTeacherAssignments(Long yearId, Long classId, Long sectionId) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void removeClassTeacher(Long assignmentId) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public SubjectAssignmentResponse assignSubject(SubjectAssignmentRequest request) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public SubjectAssignmentResponse updateSubjectAssignment(Long assignmentId, SubjectAssignmentRequest request) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public List<SubjectAssignmentResponse> getSubjectAssignments(Long yearId, Long classId, Long sectionId) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void removeSubjectAssignment(Long assignmentId) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public TeacherWorkloadResponse getTeacherWorkload(Long teacherId, Long academicYearId) {
        throw new UnsupportedOperationException(MSG);
    }
}
