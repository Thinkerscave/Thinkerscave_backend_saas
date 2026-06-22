package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.EventType;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "academic_calendar_event",
        indexes = {
                @Index(name = "idx_event_date", columnList = "start_date"),
                @Index(name = "idx_event_type", columnList = "event_type")
        }
)
public class AcademicCalendarEvent extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    @EqualsAndHashCode.Include
    private Long eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @NotBlank
    @Size(max = 150)
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "all_day")
    private Boolean allDay = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "active")
    private Boolean active = true;
}