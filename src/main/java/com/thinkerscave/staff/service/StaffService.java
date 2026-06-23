package com.thinkerscave.staff.service;

import com.thinkerscave.staff.dto.request.StaffCreateRequest;
import com.thinkerscave.staff.dto.request.StaffUpdateRequest;
import com.thinkerscave.staff.dto.response.StaffCreateResponse;
import com.thinkerscave.staff.dto.response.StaffDashboardResponse;
import com.thinkerscave.staff.dto.response.StaffDetailResponse;
import com.thinkerscave.staff.dto.response.StaffSummaryResponse;
import com.thinkerscave.staff.enums.EmploymentCategory;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.enums.StaffType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StaffService {

    StaffCreateResponse createStaff(StaffCreateRequest request);

    void updateStaff(Long staffId, StaffUpdateRequest request);

    StaffDetailResponse getStaffDetail(Long staffId);

    Page<StaffSummaryResponse> getStaffList(
            StaffType staffType,
            EmploymentCategory employmentCategory,
            EmploymentStatus employmentStatus,
            String designation,
            String keyword,
            Pageable pageable
    );

    StaffDashboardResponse getDashboard();

    void activateStaff(Long staffId);

    void deactivateStaff(Long staffId);

    void deleteStaff(Long staffId);
}
