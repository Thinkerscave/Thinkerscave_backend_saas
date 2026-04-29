package com.thinkerscave.common.course.dto;
 
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.time.LocalDate;
 
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AcademicYearDTO {
    private Long academicYearId;
    private String yearCode;
    private String yearName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;
    private Boolean isActive;
    private String description;
}
