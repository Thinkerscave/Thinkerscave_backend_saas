package com.thinkerscave.attendance.service.impl;

import com.thinkerscave.attendance.dto.request.AttendanceSettingRequest;
import com.thinkerscave.attendance.dto.response.AttendanceSettingResponse;
import com.thinkerscave.attendance.entity.AttendanceSetting;
import com.thinkerscave.attendance.enums.AttendanceMode;
import com.thinkerscave.attendance.repository.AttendanceSettingRepository;
import com.thinkerscave.attendance.service.AttendanceSettingService;
import com.thinkerscave.shared.context.OrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AttendanceSettingServiceImpl implements AttendanceSettingService {

    private final AttendanceSettingRepository attendanceSettingRepository;

    @Override
    public AttendanceSettingResponse getSettings() {
        Long orgId = OrganizationContext.getOrganizationId();
        AttendanceSetting setting = attendanceSettingRepository.findByOrganizationId(orgId)
                .orElseGet(() -> defaultSettings(orgId));
        return toResponse(setting);
    }

    @Override
    @Transactional
    public AttendanceSettingResponse saveSettings(AttendanceSettingRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        AttendanceSetting setting = attendanceSettingRepository.findByOrganizationId(orgId)
                .orElseGet(() -> {
                    AttendanceSetting s = new AttendanceSetting();
                    s.setOrganizationId(orgId);
                    return s;
                });

        if (request.getAttendanceMode() != null) setting.setAttendanceMode(request.getAttendanceMode());
        if (request.getLateAfterTime() != null) setting.setLateAfterTime(request.getLateAfterTime());
        if (request.getWindowStartTime() != null) setting.setWindowStartTime(request.getWindowStartTime());
        if (request.getWindowEndTime() != null) setting.setWindowEndTime(request.getWindowEndTime());
        if (request.getAllowCopyPrevious() != null) setting.setAllowCopyPrevious(request.getAllowCopyPrevious());
        if (request.getMinStudentAttendancePercent() != null)
            setting.setMinStudentAttendancePercent(request.getMinStudentAttendancePercent());
        if (request.getStudentAlertThresholdPercent() != null)
            setting.setStudentAlertThresholdPercent(request.getStudentAlertThresholdPercent());
        if (request.getSendSmsOnAbsent() != null) setting.setSendSmsOnAbsent(request.getSendSmsOnAbsent());
        if (request.getSendEmailOnAbsent() != null) setting.setSendEmailOnAbsent(request.getSendEmailOnAbsent());
        if (request.getMinStaffWorkingHours() != null)
            setting.setMinStaffWorkingHours(request.getMinStaffWorkingHours());
        if (request.getStaffLateGraceMinutes() != null)
            setting.setStaffLateGraceMinutes(request.getStaffLateGraceMinutes());
        if (request.getFreezeAfterDays() != null) setting.setFreezeAfterDays(request.getFreezeAfterDays());

        return toResponse(attendanceSettingRepository.save(setting));
    }

    @Override
    @Transactional
    public AttendanceSettingResponse resetToDefaults() {
        Long orgId = OrganizationContext.getOrganizationId();
        AttendanceSetting setting = attendanceSettingRepository.findByOrganizationId(orgId)
                .orElseGet(() -> {
                    AttendanceSetting s = new AttendanceSetting();
                    s.setOrganizationId(orgId);
                    return s;
                });

        setting.setAttendanceMode(AttendanceMode.DAILY);
        setting.setLateAfterTime(LocalTime.of(8, 15));
        setting.setWindowStartTime(LocalTime.of(7, 0));
        setting.setWindowEndTime(LocalTime.of(9, 0));
        setting.setAllowCopyPrevious(true);
        setting.setMinStudentAttendancePercent(75);
        setting.setStudentAlertThresholdPercent(80);
        setting.setSendSmsOnAbsent(false);
        setting.setSendEmailOnAbsent(false);
        setting.setMinStaffWorkingHours(8);
        setting.setStaffLateGraceMinutes(15);
        setting.setFreezeAfterDays(0);
        setting.setActive(true);

        return toResponse(attendanceSettingRepository.save(setting));
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private AttendanceSetting defaultSettings(Long orgId) {
        AttendanceSetting s = new AttendanceSetting();
        s.setOrganizationId(orgId);
        s.setAttendanceMode(AttendanceMode.DAILY);
        s.setLateAfterTime(LocalTime.of(8, 15));
        s.setWindowStartTime(LocalTime.of(7, 0));
        s.setWindowEndTime(LocalTime.of(9, 0));
        s.setAllowCopyPrevious(true);
        s.setMinStudentAttendancePercent(75);
        s.setStudentAlertThresholdPercent(80);
        s.setSendSmsOnAbsent(false);
        s.setSendEmailOnAbsent(false);
        s.setMinStaffWorkingHours(8);
        s.setStaffLateGraceMinutes(15);
        s.setFreezeAfterDays(0);
        s.setActive(true);
        return s;
    }

    private AttendanceSettingResponse toResponse(AttendanceSetting s) {
        return AttendanceSettingResponse.builder()
                .settingId(s.getSettingId())
                .organizationId(s.getOrganizationId())
                .attendanceMode(s.getAttendanceMode())
                .lateAfterTime(s.getLateAfterTime())
                .windowStartTime(s.getWindowStartTime())
                .windowEndTime(s.getWindowEndTime())
                .allowCopyPrevious(s.getAllowCopyPrevious())
                .minStudentAttendancePercent(s.getMinStudentAttendancePercent())
                .studentAlertThresholdPercent(s.getStudentAlertThresholdPercent())
                .sendSmsOnAbsent(s.getSendSmsOnAbsent())
                .sendEmailOnAbsent(s.getSendEmailOnAbsent())
                .minStaffWorkingHours(s.getMinStaffWorkingHours())
                .staffLateGraceMinutes(s.getStaffLateGraceMinutes())
                .freezeAfterDays(s.getFreezeAfterDays())
                .active(s.getActive())
                .build();
    }
}
