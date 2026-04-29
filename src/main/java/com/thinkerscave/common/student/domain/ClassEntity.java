package com.thinkerscave.common.student.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "class")
public class ClassEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    @EqualsAndHashCode.Include
    private Long classId;

    @Column(name = "class_name", nullable = false)
    private String className;

    @Column(name = "organization_id")
    private Long organizationId;
}