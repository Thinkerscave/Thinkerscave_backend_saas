package com.thinkerscave.common.admission.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.thinkerscave.common.admission.domain.Inquiry;

import com.thinkerscave.common.admission.enums.InquiryStatus;
import java.time.LocalDate;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry,Long> {
	
	Optional<Inquiry> findByInquiryIdAndIsDeletedFalse(Long inquiryId);

    List<Inquiry> findAllByIsDeletedFalseOrderByCreatedDateDesc();

    boolean existsByMobileNumberAndIsDeletedFalse(String mobileNumber);

    List<Inquiry> findByAssignedCounselorIdAndNextFollowUpDateAndIsDeletedFalse(Long counselorId, LocalDate date);

    List<Inquiry> findByAssignedCounselorIdAndNextFollowUpDateBeforeAndIsDeletedFalse(Long counselorId, LocalDate date);

    List<Inquiry> findByAssignedCounselorIdAndNextFollowUpDateAfterAndIsDeletedFalse(Long counselorId, LocalDate date);

    List<Inquiry> findByAssignedCounselorIdAndStatusAndIsDeletedFalse(Long counselorId, InquiryStatus status);

    List<Inquiry> findByAssignedCounselorIdAndIsDeletedFalse(Long counselorId);

}
