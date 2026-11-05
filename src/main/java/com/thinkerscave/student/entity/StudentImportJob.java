package com.thinkerscave.student.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "student_import_job")
public class StudentImportJob extends Auditable {

    @Id
    @Column(name = "job_id", length = 36)
    private String jobId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "total_records", nullable = false)
    private Integer totalRecords;

    @Column(name = "success_count", nullable = false)
    private Integer successCount;

    @Column(name = "failure_count", nullable = false)
    private Integer failureCount;

    @Column(name = "error_report", columnDefinition = "TEXT")
    private String errorReport;

    @Column(name = "completed_on")
    private LocalDateTime completedOn;
}
