package com.thinkerscave.attendance.service.impl;

import com.thinkerscave.attendance.dto.request.AttendanceFreezeRequest;
import com.thinkerscave.attendance.dto.response.AttendanceFreezeResponse;
import com.thinkerscave.attendance.entity.AttendanceFreeze;
import com.thinkerscave.attendance.repository.AttendanceFreezeRepository;
import com.thinkerscave.attendance.service.AttendanceFreezeService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AttendanceFreezeServiceImpl implements AttendanceFreezeService {

    private final AttendanceFreezeRepository attendanceFreezeRepository;

    @Override
    @Transactional
    public AttendanceFreezeResponse createFreeze(AttendanceFreezeRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();

        if (request.getFreezeFromDate().isAfter(request.getFreezeToDate())) {
            throw new BadRequestException("Freeze from date must be before or equal to to date");
        }

        AttendanceFreeze freeze = new AttendanceFreeze();
        freeze.setOrganizationId(orgId);
        freeze.setFreezeFromDate(request.getFreezeFromDate());
        freeze.setFreezeToDate(request.getFreezeToDate());
        freeze.setReason(request.getReason());
        freeze.setActive(true);

        return toResponse(attendanceFreezeRepository.save(freeze));
    }

    @Override
    public List<AttendanceFreezeResponse> getAllFreezes() {
        Long orgId = OrganizationContext.getOrganizationId();
        return attendanceFreezeRepository
                .findByOrganizationIdAndActiveTrueOrderByFreezeFromDateDesc(orgId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteFreeze(Long freezeId) {
        Long orgId = OrganizationContext.getOrganizationId();
        AttendanceFreeze freeze = attendanceFreezeRepository.findById(freezeId)
                .filter(f -> f.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Freeze record not found: " + freezeId));

        freeze.setActive(false);
        attendanceFreezeRepository.save(freeze);
        log.info("Attendance freeze {} deactivated for org {}", freezeId, orgId);
    }

    @Override
    public boolean isDateFrozen(Long organizationId, LocalDate date) {
        return attendanceFreezeRepository.isDateFrozen(organizationId, date);
    }

    private AttendanceFreezeResponse toResponse(AttendanceFreeze freeze) {
        return AttendanceFreezeResponse.builder()
                .freezeId(freeze.getFreezeId())
                .organizationId(freeze.getOrganizationId())
                .freezeFromDate(freeze.getFreezeFromDate())
                .freezeToDate(freeze.getFreezeToDate())
                .reason(freeze.getReason())
                .active(freeze.getActive())
                .build();
    }
}
