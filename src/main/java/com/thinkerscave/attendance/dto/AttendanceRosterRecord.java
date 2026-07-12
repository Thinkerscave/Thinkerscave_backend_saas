package com.thinkerscave.attendance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Flat attendance record used by the frontend roster workspace.
 * Maps to/from {@link com.thinkerscave.attendance.entity.StudentAttendance}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceRosterRecord {

    private Long attendanceId;

    /** CLASS | STAFF | HOSTEL */
    private String attendanceType;

    /** studentId (for CLASS) or staffId (for STAFF) */
    private Long referenceId;

    /** student or staff full name */
    private String referenceName;

    /** ISO date string yyyy-MM-dd */
    private String attendanceDate;

    /** PRESENT | ABSENT | LATE | EXCUSED | HALF_DAY */
    private String status;

    private Long classId;
    private String className;
    private String sectionName;
    private String shift;
    private String department;
    private String remarks;
}
