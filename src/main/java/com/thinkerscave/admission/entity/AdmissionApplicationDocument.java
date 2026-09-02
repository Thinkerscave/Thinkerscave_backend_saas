package com.thinkerscave.admission.entity;

import com.thinkerscave.admission.enums.DocumentCheckStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(
        name = "admission_application_document",
        indexes = {
                @Index(name = "idx_aad_application", columnList = "application_id")
        }
)
public class AdmissionApplicationDocument extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    @EqualsAndHashCode.Include
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private ApplicationAdmission application;

    @Column(name = "document_type", nullable = false, length = 80)
    private String documentType;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "stored_path", length = 500)
    private String storedPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DocumentCheckStatus status = DocumentCheckStatus.PENDING;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
