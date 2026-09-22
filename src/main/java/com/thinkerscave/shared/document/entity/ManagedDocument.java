package com.thinkerscave.shared.document.entity;

import com.thinkerscave.shared.document.enums.DocumentType;
import com.thinkerscave.shared.document.enums.ManagedDocumentStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "managed_document", indexes = {
        @Index(name = "idx_managed_document_type_period", columnList = "document_type,period_key"),
        @Index(name = "idx_managed_document_owner", columnList = "owner_type,owner_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_managed_document_idem",
                columnNames = {"document_type", "owner_type", "owner_id", "idempotency_key"})
})
public class ManagedDocument extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "managed_document_id")
    @EqualsAndHashCode.Include
    private Long managedDocumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private DocumentType documentType;

    @Column(name = "owner_type", nullable = false, length = 40)
    private String ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "period_key", length = 20)
    private String periodKey;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "byte_size", nullable = false)
    private Long byteSize;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ManagedDocumentStatus status = ManagedDocumentStatus.ACTIVE;
}
