package com.thinkerscave.common.student.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "class", indexes = {
    @Index(name = "idx_class_org", columnList = "organization_id")
})
public class ClassEntity extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    @EqualsAndHashCode.Include
    private Long classId;

    @Column(name = "class_name", nullable = false)
    private String className;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}