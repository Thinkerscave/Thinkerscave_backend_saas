package com.thinkerscave.common.staff.repository;


import com.thinkerscave.common.staff.domain.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff,Long> {
}
