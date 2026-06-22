package com.thinkerscave.academics.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "syllabus",
        indexes = {
                @Index(name = "idx_syllabus_subject", columnList = "subject_id")
        }
)
public class Syllabus extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "syllabus_id")
    @EqualsAndHashCode.Include
    private Long syllabusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private AcademicClass academicClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @NotBlank
    @Size(max = 150)
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "version_no", length = 20)
    private String versionNo;

    @Column(name = "published")
    private Boolean published = false;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}