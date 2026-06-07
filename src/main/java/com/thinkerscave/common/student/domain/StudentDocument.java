package com.thinkerscave.common.student.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Entity
@Data

@Table(name = "student_document")
public class StudentDocument extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "document_name", nullable = false)
    private String documentName;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "document_path", nullable = false)
    private String documentPath;

    @Column(name = "category", length = 40)
    private String category;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(name = "verified_on")
    private LocalDate verifiedOn;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "organization_id")
    private Long organizationId;

}
