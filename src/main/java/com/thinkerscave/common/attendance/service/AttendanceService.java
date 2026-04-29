package com.thinkerscave.common.attendance.service;

import com.thinkerscave.common.attendance.domain.Attendance.AttendanceType;
import com.thinkerscave.common.attendance.dto.AttendanceRequestDTO;
import com.thinkerscave.common.attendance.dto.AttendanceResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {
    AttendanceResponseDTO save(AttendanceRequestDTO dto, String markedBy);

    AttendanceResponseDTO update(Long id, AttendanceRequestDTO dto);

    void delete(Long id);

    List<AttendanceResponseDTO> getByDateAndType(LocalDate date, AttendanceType type);

    List<AttendanceResponseDTO> getByClassAndDate(Long classId, LocalDate date);

    List<AttendanceResponseDTO> getByReferenceId(Long referenceId, AttendanceType type);
}
