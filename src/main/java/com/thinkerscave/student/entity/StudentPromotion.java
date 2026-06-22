package com.thinkerscave.student.entity;

import java.time.LocalDate;

import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.shared.entity.Auditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "student_promotion")
public class StudentPromotion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "promotion_id")
    private Long promotionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_class_id")
    private AcademicClass fromClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_class_id")
    private AcademicClass toClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_section_id")
    private AcademicSection fromSection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_section_id")
    private AcademicSection toSection;

    @Column(name = "promotion_date")
    private LocalDate promotionDate;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
