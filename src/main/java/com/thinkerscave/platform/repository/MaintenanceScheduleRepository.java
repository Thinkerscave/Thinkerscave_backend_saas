package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.MaintenanceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, Long> {

    List<MaintenanceSchedule> findByActiveTrueOrderByStartTimeDesc();

    List<MaintenanceSchedule> findByOrganization_IdAndActiveTrueOrderByStartTimeDesc(Long organizationId);

    @Query("""
            SELECT m FROM MaintenanceSchedule m
            WHERE m.active = true
            AND m.completed = false
            AND m.startTime >= :from
            ORDER BY m.startTime ASC
            """)
    List<MaintenanceSchedule> findUpcoming(@Param("from") LocalDateTime from);
}
