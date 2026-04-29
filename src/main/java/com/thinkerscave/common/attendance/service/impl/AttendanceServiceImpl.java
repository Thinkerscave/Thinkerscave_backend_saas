package com.thinkerscave.common.attendance.service.impl;

import com.thinkerscave.common.attendance.domain.Attendance;
import com.thinkerscave.common.attendance.domain.Attendance.AttendanceType;
import com.thinkerscave.common.attendance.dto.AttendanceRequestDTO;
import com.thinkerscave.common.attendance.dto.AttendanceResponseDTO;
import com.thinkerscave.common.attendance.repository.AttendanceRepository;
import com.thinkerscave.common.attendance.service.AttendanceService;
import com.thinkerscave.common.context.OrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;

    /**
     * Returns the current organization ID from the request context.
     * Throws if no org context is set — callers must have X-Organization-ID
     * resolved by OrganizationFilter.
     */
    private Long requireOrgId() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null) {
            throw new IllegalStateException(
                    "No organization context set. Ensure X-Organization-ID header is provided or auto-detected.");
        }
        return orgId;
    }

    @Override
    @Transactional
    public AttendanceResponseDTO save(AttendanceRequestDTO dto, String markedBy) {
        Long orgId = requireOrgId();

        Attendance attendance = Attendance.builder()
                .organizationId(orgId)
                .attendanceType(dto.getAttendanceType())
                .referenceId(dto.getReferenceId())
                .referenceName(dto.getReferenceName())
                .attendanceDate(dto.getAttendanceDate())
                .status(dto.getStatus())
                .classId(dto.getClassId())
                .className(dto.getClassName())
                .sectionName(dto.getSectionName())
                .shift(dto.getShift())
                .department(dto.getDepartment())
                .roomNumber(dto.getRoomNumber())
                .remarks(dto.getRemarks())
                .markedBy(markedBy)
                .build();

        return toDTO(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional
    public AttendanceResponseDTO update(Long id, AttendanceRequestDTO dto) {
        Long orgId = requireOrgId();

        Attendance existing = attendanceRepository.findById(id)
                .filter(a -> orgId.equals(a.getOrganizationId()))
                .orElseThrow(() -> new RuntimeException("Attendance record not found or access denied: " + id));

        if (dto.getStatus() != null)
            existing.setStatus(dto.getStatus());
        if (dto.getShift() != null)
            existing.setShift(dto.getShift());
        if (dto.getRemarks() != null)
            existing.setRemarks(dto.getRemarks());

        return toDTO(attendanceRepository.save(existing));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Long orgId = requireOrgId();
        Attendance existing = attendanceRepository.findById(id)
                .filter(a -> orgId.equals(a.getOrganizationId()))
                .orElseThrow(() -> new RuntimeException("Attendance record not found or access denied: " + id));
        attendanceRepository.delete(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getByDateAndType(LocalDate date, AttendanceType type) {
        Long orgId = requireOrgId();
        return attendanceRepository
                .findByOrganizationIdAndAttendanceDateAndAttendanceType(orgId, date, type)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getByClassAndDate(Long classId, LocalDate date) {
        Long orgId = requireOrgId();
        return attendanceRepository
                .findByOrganizationIdAndClassIdAndAttendanceDate(orgId, classId, date)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getByReferenceId(Long referenceId, AttendanceType type) {
        Long orgId = requireOrgId();
        return attendanceRepository
                .findByOrganizationIdAndReferenceIdAndAttendanceType(orgId, referenceId, type)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private AttendanceResponseDTO toDTO(Attendance a) {
        return AttendanceResponseDTO.builder()
                .id(a.getId())
                .organizationId(a.getOrganizationId())
                .attendanceType(a.getAttendanceType())
                .referenceId(a.getReferenceId())
                .referenceName(a.getReferenceName())
                .attendanceDate(a.getAttendanceDate())
                .status(a.getStatus())
                .classId(a.getClassId())
                .className(a.getClassName())
                .sectionName(a.getSectionName())
                .shift(a.getShift())
                .department(a.getDepartment())
                .roomNumber(a.getRoomNumber())
                .remarks(a.getRemarks())
                .markedBy(a.getMarkedBy())
                .createdAt(a.getCreatedDate() != null
                        ? a.getCreatedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        : null)
                .build();
    }
}
