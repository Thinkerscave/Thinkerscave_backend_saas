package com.thinkerscave.common.staff.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "staff_document", indexes = {
        @Index(name = "idx_staff_document_org", columnList = "organization_id"),
        @Index(name = "idx_staff_document_staff", columnList = "staff_id"),
        @Index(name = "idx_staff_document_category", columnList = "category")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffDocument extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "category", nullable = false, length = 40)
    private String category;

    @Column(name = "document_type", nullable = false, length = 80)
    private String documentType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(name = "verified_on")
    private LocalDate verifiedOn;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
