package com.thinkerscave.document.entity;

import com.thinkerscave.document.enums.DocumentOwnerType;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "document",
        indexes = {
                @Index(name = "idx_document_owner", columnList = "owner_type, owner_id"),
                @Index(name = "idx_document_type", columnList = "document_type")
        }
)
public class Document extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    @EqualsAndHashCode.Include
    private Long documentId;

    /**
     * STUDENT
     * STAFF
     * PARENT
     * ORGANIZATION
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 50)
    private DocumentOwnerType ownerType;

    /**
     * StudentId
     * StaffId
     * ParentId
     */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * BIRTH_CERTIFICATE
     * AADHAR
     * PHOTO
     * RESUME
     */
    @Column(name = "document_type", nullable = false, length = 100)
    private String documentType;

    @Column(name = "document_name", nullable = false, length = 255)
    private String documentName;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "file_extension", length = 20)
    private String fileExtension;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "active")
    private Boolean active = true;
}