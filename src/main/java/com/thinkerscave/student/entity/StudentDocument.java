package com.thinkerscave.student.entity;

import com.thinkerscave.admission.enums.DocumentCheckStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "student_document")
public class StudentDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "document_name", nullable = false, length = 255)
    private String documentName;

    @Column(name = "document_type", nullable = false, length = 255)
    private String documentType;

    @Column(name = "document_path", nullable = false, length = 255)
    private String documentPath;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_extension", length = 20)
    private String fileExtension;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @CreationTimestamp
    @Column(name = "uploaded_on", nullable = false, updatable = false)
    private LocalDateTime uploadedOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DocumentCheckStatus status = DocumentCheckStatus.PENDING;

    @Column(name = "remarks", length = 500)
    private String remarks;

    /** Optional link back to the admissions document that was copied on enroll. */
    @Column(name = "source_application_document_id")
    private Long sourceApplicationDocumentId;

    @Column(name = "replaced_document_id")
    private Long replacedDocumentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
