package com.thinkerscave.common.admission.repository;

import com.thinkerscave.common.admission.domain.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {
    List<FollowUp> findByInquiry_InquiryIdOrderByFollowUpDateDesc(Long inquiryId);
}
