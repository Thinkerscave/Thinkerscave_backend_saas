package com.thinkerscave.common.course.service;

import com.thinkerscave.common.course.dto.TeacherAllocationDTO;
import java.util.List;

public interface TeacherAllocationService {

    TeacherAllocationDTO allocateTeacher(TeacherAllocationDTO dto);

    TeacherAllocationDTO updateAllocation(Long allocationId, TeacherAllocationDTO dto);

    void deallocateTeacher(Long allocationId);

    List<TeacherAllocationDTO> getAllocationsByClass(Long classId, Long academicYearId);

    List<TeacherAllocationDTO> getAllocationsByTeacher(Long teacherId, Long academicYearId);
}
