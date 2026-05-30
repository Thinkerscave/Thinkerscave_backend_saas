package com.thinkerscave.common.enrollment.mapper;

import com.thinkerscave.common.enrollment.domain.AcademicEnrollment;
import com.thinkerscave.common.enrollment.dto.AcademicEnrollmentDTO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface EnrollmentMapper {

    AcademicEnrollmentDTO toDto(AcademicEnrollment enrollment);

    List<AcademicEnrollmentDTO> toDtoList(List<AcademicEnrollment> enrollments);

    AcademicEnrollment toEntity(AcademicEnrollmentDTO dto);
}
