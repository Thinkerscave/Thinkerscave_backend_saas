package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.TeacherArrangementStatus;
import com.thinkerscave.shared.entity.Auditable;
import com.thinkerscave.staff.entity.Staff;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "academics_teacher_arrangement",
        indexes = {
                @Index(name = "idx_academics_teacher_arrangement_year_date", columnList = "academic_year_id, absence_date"),
                @Index(name = "idx_academics_teacher_arrangement_status", columnList = "status")
        }
)
public class TeacherArrangement extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "teacher_arrangement_id")
    @EqualsAndHashCode.Include
    private Long teacherArrangementId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timetable_version_id", nullable = false)
    private TimetableVersion timetableVersion;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "absent_teacher_staff_id", nullable = false)
    private Staff absentTeacher;

    @NotNull
    @Column(name = "absence_date", nullable = false)
    private LocalDate absenceDate;

    @NotNull
    @Size(max = 500)
    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TeacherArrangementStatus status = TeacherArrangementStatus.PENDING;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Size(max = 500)
    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @OneToMany(mappedBy = "arrangement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeacherArrangementSlot> slots = new ArrayList<>();
}