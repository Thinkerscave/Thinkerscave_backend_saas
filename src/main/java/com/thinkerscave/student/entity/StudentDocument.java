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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DocumentCheckStatus status = DocumentCheckStatus.PENDING;

    @Column(name = "remarks", length = 500)
    private String remarks;

    /** Optional link back to the admissions document that was copied on enroll. */
    @Column(name = "source_application_document_id")
    private Long sourceApplicationDocumentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;
}
