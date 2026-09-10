package com.thinkerscave.admission.repository;

import com.thinkerscave.admission.entity.InquiryFollowUp;
import com.thinkerscave.admission.enums.FollowUpLifecycleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InquiryFollowUpRepository extends JpaRepository<InquiryFollowUp, Long> {

    List<InquiryFollowUp> findByInquiryInquiryIdOrderByFollowUpDateDesc(Long inquiryId);

    /** Tenant-wide (KPI / workspace). Prefer counselor-scoped methods for the work queue. */
    @Query("""
        SELECT f
        FROM InquiryFollowUp f
        JOIN f.inquiry i
        WHERE i.deleted = false
          AND (f.lifecycleStatus IS NULL
               OR f.lifecycleStatus = com.thinkerscave.admission.enums.FollowUpLifecycleStatus.SCHEDULED
               OR f.lifecycleStatus = com.thinkerscave.admission.enums.FollowUpLifecycleStatus.RESCHEDULED)
          AND f.nextFollowUpDate = :date
        ORDER BY f.nextFollowUpDate ASC, f.followUpDate DESC
        """)
    List<InquiryFollowUp> findDueOnDate(@Param("date") java.time.LocalDate date);

    @Query("""
        SELECT f
        FROM InquiryFollowUp f
        JOIN f.inquiry i
        WHERE i.deleted = false
          AND (f.lifecycleStatus IS NULL
               OR f.lifecycleStatus = com.thinkerscave.admission.enums.FollowUpLifecycleStatus.SCHEDULED
               OR f.lifecycleStatus = com.thinkerscave.admission.enums.FollowUpLifecycleStatus.RESCHEDULED)
          AND f.nextFollowUpDate < :date
        ORDER BY f.nextFollowUpDate ASC, f.followUpDate DESC
        """)
    List<InquiryFollowUp> findOverdue(@Param("date") java.time.LocalDate date);

    /**
     * Counselor work queue — Today: SCHEDULED follow-ups whose planned slot falls on [dayStart, dayEnd).
     */
    @Query("""
        SELECT f
        FROM InquiryFollowUp f
        JOIN f.inquiry i
        WHERE i.deleted = false
          AND i.assignedCounselorId = :counselorId
          AND (f.lifecycleStatus IS NULL
               OR f.lifecycleStatus = com.thinkerscave.admission.enums.FollowUpLifecycleStatus.SCHEDULED
               OR f.lifecycleStatus = com.thinkerscave.admission.enums.FollowUpLifecycleStatus.RESCHEDULED)
          AND f.followUpDate >= :dayStart
          AND f.followUpDate < :dayEnd
        ORDER BY f.followUpDate ASC
        """)
    List<InquiryFollowUp> findTodayForCounselor(
            @Param("counselorId") Long counselorId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);

    /**
     * Counselor work queue — Overdue: SCHEDULED follow-ups planned before today.
     */
    @Query("""
        SELECT f
        FROM InquiryFollowUp f
        JOIN f.inquiry i
        WHERE i.deleted = false
          AND i.assignedCounselorId = :counselorId
          AND (f.lifecycleStatus IS NULL
               OR f.lifecycleStatus = com.thinkerscave.admission.enums.FollowUpLifecycleStatus.SCHEDULED
               OR f.lifecycleStatus = com.thinkerscave.admission.enums.FollowUpLifecycleStatus.RESCHEDULED)
          AND f.followUpDate < :dayStart
        ORDER BY f.followUpDate ASC
        """)
    List<InquiryFollowUp> findOverdueForCounselor(
            @Param("counselorId") Long counselorId,
            @Param("dayStart") LocalDateTime dayStart);

    /**
     * Counselor work queue — Upcoming: SCHEDULED follow-ups planned after today.
     */
    @Query("""
        SELECT f
        FROM InquiryFollowUp f
        JOIN f.inquiry i
        WHERE i.deleted = false
          AND i.assignedCounselorId = :counselorId
          AND (f.lifecycleStatus IS NULL
               OR f.lifecycleStatus = com.thinkerscave.admission.enums.FollowUpLifecycleStatus.SCHEDULED
               OR f.lifecycleStatus = com.thinkerscave.admission.enums.FollowUpLifecycleStatus.RESCHEDULED)
          AND f.followUpDate >= :dayEnd
        ORDER BY f.followUpDate ASC
        """)
    List<InquiryFollowUp> findUpcomingForCounselor(
            @Param("counselorId") Long counselorId,
            @Param("dayEnd") LocalDateTime dayEnd);

    /**
     * Counselor work queue — Completed: completed follow-ups for leads assigned to the counselor.
     */
    @Query("""
        SELECT f
        FROM InquiryFollowUp f
        JOIN f.inquiry i
        WHERE i.deleted = false
          AND i.assignedCounselorId = :counselorId
          AND f.lifecycleStatus = com.thinkerscave.admission.enums.FollowUpLifecycleStatus.COMPLETED
        ORDER BY f.completedOn DESC, f.followUpDate DESC
        """)
    List<InquiryFollowUp> findCompletedForCounselor(@Param("counselorId") Long counselorId);

    boolean existsByInquiryInquiryIdAndLifecycleStatus(Long inquiryId, FollowUpLifecycleStatus lifecycleStatus);

    @Query("""
        SELECT COUNT(f)
        FROM InquiryFollowUp f
        JOIN f.inquiry i
        WHERE i.deleted = false
          AND i.assignedCounselorId = :counselorId
          AND (f.lifecycleStatus IS NULL
               OR f.lifecycleStatus = com.thinkerscave.admission.enums.FollowUpLifecycleStatus.SCHEDULED
               OR f.lifecycleStatus = com.thinkerscave.admission.enums.FollowUpLifecycleStatus.RESCHEDULED)
          AND f.followUpDate >= :dayStart
          AND f.followUpDate < :dayEnd
        """)
    long countScheduledForCounselorBetween(
            @Param("counselorId") Long counselorId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);
}
