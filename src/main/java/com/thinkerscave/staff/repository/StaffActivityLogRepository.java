package com.thinkerscave.staff.repository;

import com.thinkerscave.staff.entity.StaffActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffActivityLogRepository extends JpaRepository<StaffActivityLog, Long> {

    List<StaffActivityLog> findTop100ByStaff_StaffIdOrderByPerformedOnDescActivityIdDesc(Long staffId);
}
