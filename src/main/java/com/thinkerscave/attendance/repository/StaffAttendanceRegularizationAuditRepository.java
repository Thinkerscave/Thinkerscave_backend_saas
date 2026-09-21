package com.thinkerscave.attendance.repository;

import com.thinkerscave.attendance.entity.StaffAttendanceRegularizationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffAttendanceRegularizationAuditRepository
        extends JpaRepository<StaffAttendanceRegularizationAudit, Long> {

    List<StaffAttendanceRegularizationAudit> findByOrganizationIdAndRequestIdOrderByActionAtDesc(
            Long organizationId, Long requestId);
}
