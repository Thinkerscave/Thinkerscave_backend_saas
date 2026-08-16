package com.thinkerscave.student.entity;

import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.student.enums.EnrollmentStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "student_enrollment",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_student_academic_year",
                        columnNames = {
                                "student_id",
                                "academic_year_id"
                        }
                )
        }
)
public class StudentEnrollment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrollment_id")
    @EqualsAndHashCode.Include
    private Long enrollmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private AcademicClass classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private AcademicSection section;

    @Column(name = "roll_number", length = 50)
    private String rollNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}