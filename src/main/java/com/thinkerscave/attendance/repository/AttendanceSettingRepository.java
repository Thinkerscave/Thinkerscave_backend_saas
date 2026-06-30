package com.thinkerscave.attendance.repository;

import com.thinkerscave.attendance.entity.AttendanceSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttendanceSettingRepository extends JpaRepository<AttendanceSetting, Long> {

    Optional<AttendanceSetting> findByOrganizationId(Long organizationId);

    boolean existsByOrganizationId(Long organizationId);
}
