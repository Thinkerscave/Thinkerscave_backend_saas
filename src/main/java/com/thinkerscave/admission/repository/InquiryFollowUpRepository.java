package com.thinkerscave.admission.repository;

import com.thinkerscave.admission.entity.InquiryFollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InquiryFollowUpRepository extends JpaRepository<InquiryFollowUp, Long> {

    List<InquiryFollowUp> findByInquiryInquiryIdOrderByFollowUpDateDesc(Long inquiryId);

    @Query("""
        SELECT f
        FROM InquiryFollowUp f
        JOIN f.inquiry i
        WHERE i.deleted = false
          AND f.nextFollowUpDate = :date
        ORDER BY f.nextFollowUpDate ASC, f.followUpDate DESC
        """)
    List<InquiryFollowUp> findDueOnDate(@Param("date") LocalDate date);

    @Query("""
        SELECT f
        FROM InquiryFollowUp f
        JOIN f.inquiry i
        WHERE i.deleted = false
          AND f.nextFollowUpDate < :date
        ORDER BY f.nextFollowUpDate ASC, f.followUpDate DESC
        """)
    List<InquiryFollowUp> findOverdue(@Param("date") LocalDate date);
}
