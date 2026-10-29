package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.response.TeacherWorkloadDashboardResponse;
import com.thinkerscave.academics.service.TeacherWorkloadService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/academics/years/{academicYearId}/teacher-workload")
@RequiredArgsConstructor
@Tag(name = "Teacher Allocation", description = "Teacher workload analytics derived from allocations and timetable patterns")
public class TeacherWorkloadController {

    private final TeacherWorkloadService teacherWorkloadService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Teacher workload dashboard for an academic year")
    public ResponseEntity<ApiResponse<TeacherWorkloadDashboardResponse>> dashboard(
            @PathVariable Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("Teacher workload retrieved",
                teacherWorkloadService.getDashboard(academicYearId)));
    }
}
