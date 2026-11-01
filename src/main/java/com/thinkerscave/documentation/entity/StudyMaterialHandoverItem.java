package com.thinkerscave.documentation.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name = "study_material_handover_item")
public class StudyMaterialHandoverItem extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @Column(name = "handover_id", nullable = false)
    private Long handoverId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", nullable = false, length = 200)
    private String studentName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "handed_over", nullable = false)
    private boolean handedOver;

    @Column(name = "status", nullable = false, length = 30)
    private String status;
}
