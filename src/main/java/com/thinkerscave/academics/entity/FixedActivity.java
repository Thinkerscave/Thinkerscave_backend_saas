package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

/** Optional recurring timetable reservation such as assembly, exercise or library hour. */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "fixed_activity",
        indexes = {
                @Index(name = "idx_fixed_activity_year_active", columnList = "academic_year_id, is_active")
        }
)
public class FixedActivity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fixed_activity_id")
    @EqualsAndHashCode.Include
    private Long fixedActivityId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @NotBlank
    @Size(max = 120)
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Size(max = 40)
    @Column(name = "activity_type", nullable = false, length = 40)
    private String activityType = "GENERAL";

    @NotNull
    @Column(name = "applies_to_all_groups", nullable = false)
    private Boolean appliesToAllGroups = true;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Size(max = 20)
    @Column(name = "recurrence", nullable = false, length = 20)
    private String recurrence = "WEEKLY";

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "fixed_activity_day",
            joinColumns = @JoinColumn(name = "fixed_activity_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 15)
    private Set<DayOfWeek> days = new LinkedHashSet<>();

    @OneToMany(mappedBy = "fixedActivity", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private Set<FixedActivityGroup> groups = new LinkedHashSet<>();

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    public boolean isEffectiveOn(LocalDate date) {
        if (date == null) return true;
        if (effectiveFrom != null && date.isBefore(effectiveFrom)) return false;
        return effectiveTo == null || !date.isAfter(effectiveTo);
    }
}
