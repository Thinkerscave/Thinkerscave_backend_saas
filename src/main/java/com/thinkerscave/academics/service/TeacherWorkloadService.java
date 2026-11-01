package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.response.TeacherWorkloadDashboardResponse;

public interface TeacherWorkloadService {

    TeacherWorkloadDashboardResponse getDashboard(Long academicYearId);
}
