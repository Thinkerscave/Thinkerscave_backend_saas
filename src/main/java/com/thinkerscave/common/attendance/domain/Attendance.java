package com.thinkerscave.common.attendance.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "attendance")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Attendance extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Organization this record belongs to — isolates data across branches in the
     * same tenant schema
     */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_type", nullable = false, length = 20)
    private AttendanceType attendanceType;

    /** Student ID, Staff ID, or Hostel Resident ID depending on type */
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_name", nullable = false, length = 255)
    private String referenceName;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;

    /** For class-specific attendance */
    @Column(name = "class_id")
    private Long classId;

    @Column(name = "class_name", length = 100)
    private String className;

    @Column(name = "section_name", length = 50)
    private String sectionName;

    /** For staff attendance */
    @Column(name = "shift", length = 20)
    private String shift;

    @Column(name = "department", length = 100)
    private String department;

    /** For hostel attendance */
    @Column(name = "room_number", length = 20)
    private String roomNumber;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "marked_by", length = 100)
    private String markedBy;

    public enum AttendanceType {
        CLASS, STAFF, HOSTEL
    }

    public enum AttendanceStatus {
        PRESENT, ABSENT, LATE, EXCUSED, WFH, ON_LEAVE, NIGHT_OUT
    }
}
