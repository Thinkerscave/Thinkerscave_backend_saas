package com.thinkerscave.admission.repository;

import com.thinkerscave.admission.entity.LeadCounselorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadCounselorAssignmentRepository extends JpaRepository<LeadCounselorAssignment, Long> {

    List<LeadCounselorAssignment> findByInquiryIdAndActiveTrueOrderByAssignedOnDesc(Long inquiryId);

    List<LeadCounselorAssignment> findByInquiryIdOrderByAssignedOnDesc(Long inquiryId);
}