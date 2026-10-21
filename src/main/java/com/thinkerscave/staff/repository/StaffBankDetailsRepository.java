package com.thinkerscave.staff.repository;

import com.thinkerscave.staff.entity.StaffBankDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffBankDetailsRepository extends JpaRepository<StaffBankDetails, Long> {

    Optional<StaffBankDetails> findByStaff_StaffId(Long staffId);
}
