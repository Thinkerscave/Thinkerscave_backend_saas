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
        name = "subject",
        indexes = {
                @Index(name = "idx_subject_code", columnList = "subject_code"),
                @Index(name = "idx_subject_name", columnList = "subject_name")
        }
)
public class Subject extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subject_id")
    @EqualsAndHashCode.Include
    private Long subjectId;

    @NotBlank
    @Size(max = 30)
    @Column(name = "subject_code", nullable = false, unique = true, length = 30)
    private String subjectCode;

    @NotBlank
    @Size(max = 100)
    @Column(name = "subject_name", nullable = false, length = 100)
    private String subjectName;

    @Size(max = 50)
    @Column(name = "subject_type", length = 50)
    private String subjectType;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}