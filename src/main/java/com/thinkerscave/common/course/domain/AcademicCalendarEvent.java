package com.thinkerscave.common.course.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.course.enums.AcademicEventType;
import com.thinkerscave.common.orgm.domain.Organisation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "academic_calendar_event", indexes = {
        @Index(name = "idx_calendar_org_year", columnList = "organization_id, academic_year_id"),
        @Index(name = "idx_calendar_dates", columnList = "start_date, end_date"),
        @Index(name = "idx_calendar_type", columnList = "event_type")
})
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "organization_id = :tenantId")
@Getter
@Setter
public class AcademicCalendarEvent extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organisation organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private AcademicEventType eventType = AcademicEventType.EVENT;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "all_day")
    private Boolean allDay = true;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String description;
}