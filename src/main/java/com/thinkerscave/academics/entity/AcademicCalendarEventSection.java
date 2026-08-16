package com.thinkerscave.academics.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(
        name = "academic_calendar_event_section",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ace_section", columnNames = {"event_id", "section_id"})
        },
        indexes = {
                @Index(name = "idx_ace_section_event", columnList = "event_id"),
                @Index(name = "idx_ace_section_section", columnList = "section_id")
        }
)
public class AcademicCalendarEventSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private AcademicCalendarEvent event;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private AcademicSection section;
}
