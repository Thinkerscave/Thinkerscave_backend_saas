package com.thinkerscave.documentation.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name = "doc_collection_master")
public class DocCollectionMaster extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "master_id")
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "master_name", nullable = false, length = 150)
    private String masterName;

    @Column(name = "collection_type", nullable = false, length = 80)
    private String collectionType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "class_ids", nullable = false, length = 400)
    private String classIds;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
}
