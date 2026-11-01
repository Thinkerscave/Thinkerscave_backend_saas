package com.thinkerscave.documentation.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name = "study_material_master")
public class StudyMaterialMaster extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "master_id")
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "master_name", nullable = false, length = 150)
    private String masterName;

    @Column(name = "material_type", nullable = false, length = 80)
    private String materialType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "class_ids", nullable = false, length = 400)
    private String classIds;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
}
