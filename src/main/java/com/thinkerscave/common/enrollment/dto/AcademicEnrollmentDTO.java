package com.thinkerscave.common.enrollment.dto;

import com.thinkerscave.common.enrollment.domain.EnrollmentStatus;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicEnrollmentDTO {
    private Long id;
    private String enrollmentNumber;
    private Long studentId;
    private Long academicYearId;
    private Long classId;
    private Long sectionId;
    private String rollNumber;
    private String house;
    private LocalDate enrollmentDate;
    private LocalDate exitDate;
    private EnrollmentStatus status;
    private String remarks;
}
