package com.thinkerscave.admission.repository;

import com.thinkerscave.admission.entity.InquiryFollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryFollowUpRepository extends JpaRepository<InquiryFollowUp, Long> {

    List<InquiryFollowUp> findByInquiryInquiryIdOrderByFollowUpDateDesc(Long inquiryId);
}
