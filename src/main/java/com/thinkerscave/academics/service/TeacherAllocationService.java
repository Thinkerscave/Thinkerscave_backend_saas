package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.ClassTeacherAssignmentRequest;
import com.thinkerscave.academics.dto.request.TeacherAllocationAssignRequest;
import com.thinkerscave.academics.dto.response.ClassTeacherAssignmentResponse;
import com.thinkerscave.academics.dto.response.TeacherAllocationDashboardResponse;
import com.thinkerscave.academics.dto.response.TeacherAllocationRowResponse;
import com.thinkerscave.academics.dto.response.TeacherRecommendationResponse;
import com.thinkerscave.academics.dto.response.TeacherWorkloadResponse;
import com.thinkerscave.academics.enums.TeacherAllocationStatus;

import java.util.List;

public interface TeacherAllocationService {

    TeacherAllocationDashboardResponse getDashboard(
            Long academicYearId,
            Long classId,
            Long sectionId,
            Long subjectId,
            TeacherAllocationStatus status);

    TeacherAllocationRowResponse assign(TeacherAllocationAssignRequest request);

    TeacherAllocationRowResponse unassign(Long teacherAllocationId);

    List<TeacherRecommendationResponse> recommendations(Long sectionId, Long classSubjectMappingId);

    TeacherWorkloadResponse getTeacherWorkload(Long staffId, Long academicYearId);

    List<TeacherWorkloadResponse> listWorkloads(Long academicYearId);

    ClassTeacherAssignmentResponse assignClassTeacher(ClassTeacherAssignmentRequest request);

    List<ClassTeacherAssignmentResponse> getClassTeachers(Long yearId, Long classId, Long sectionId);

    void removeClassTeacher(Long assignmentId);
}
