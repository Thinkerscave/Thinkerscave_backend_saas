package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.CalendarAudienceType;
import com.thinkerscave.academics.enums.CalendarEventStatus;
import com.thinkerscave.academics.enums.CalendarEventType;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "academic_calendar_event",
        indexes = {
                @Index(name = "idx_ace_year_start", columnList = "academic_year_id, start_date"),
                @Index(name = "idx_ace_year_status", columnList = "academic_year_id, status"),
                @Index(name = "idx_ace_year_type", columnList = "academic_year_id, event_type")
        }
)
public class AcademicCalendarEvent extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    @EqualsAndHashCode.Include
    private Long eventId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @NotBlank
    @Size(max = 200)
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Size(max = 2000)
    @Column(name = "description", length = 2000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private CalendarEventType eventType;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull
    @Column(name = "all_day", nullable = false)
    private Boolean allDay = true;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Size(max = 255)
    @Column(name = "location", length = 255)
    private String location;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 20)
    private CalendarAudienceType audienceType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CalendarEventStatus status = CalendarEventStatus.DRAFT;

    @Size(max = 100)
    @Column(name = "published_by", length = 100)
    private String publishedBy;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AcademicCalendarEventClass> classes = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AcademicCalendarEventSection> sections = new ArrayList<>();

    public boolean isAllDay() {
        return Boolean.TRUE.equals(allDay);
    }
}
