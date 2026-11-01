package com.thinkerscave.documentation.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name = "study_material_handover")
public class StudyMaterialHandover extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "handover_id")
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "class_name", length = 100)
    private String className;

    @Column(name = "master_id", nullable = false)
    private Long masterId;
}
