package com.thinkerscave.platform.service.impl;

import com.thinkerscave.platform.dto.request.MaintenanceScheduleRequest;
import com.thinkerscave.platform.dto.response.MaintenanceScheduleResponse;
import com.thinkerscave.platform.entity.MaintenanceSchedule;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.repository.MaintenanceScheduleRepository;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.platform.service.MaintenanceService;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceServiceImpl implements MaintenanceService {

    private final MaintenanceScheduleRepository maintenanceRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceScheduleResponse> getAllMaintenanceSchedules() {
        return maintenanceRepository.findByActiveTrueOrderByStartTimeDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MaintenanceScheduleResponse createMaintenanceSchedule(MaintenanceScheduleRequest request) {
        Organization org = null;
        if (request.getOrganizationId() != null) {
            org = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + request.getOrganizationId()));
        }
        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .organization(org)
                .title(request.getTitle())
                .description(request.getDescription())
                .reason(request.getReason())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .planned(request.getPlanned() == null || request.getPlanned())
                .notificationSent(false)
                .completed(false)
                .active(true)
                .remarks(request.getRemarks())
                .build();
        return toResponse(maintenanceRepository.save(schedule));
    }

    @Override
    @Transactional
    public MaintenanceScheduleResponse updateMaintenanceSchedule(Long id, MaintenanceScheduleRequest request) {
        MaintenanceSchedule schedule = findById(id);
        schedule.setTitle(request.getTitle());
        schedule.setDescription(request.getDescription());
        schedule.setReason(request.getReason());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        if (request.getPlanned() != null) schedule.setPlanned(request.getPlanned());
        schedule.setRemarks(request.getRemarks());
        return toResponse(maintenanceRepository.save(schedule));
    }

    @Override
    @Transactional
    public void deleteMaintenanceSchedule(Long id) {
        MaintenanceSchedule schedule = findById(id);
        schedule.setActive(false);
        maintenanceRepository.save(schedule);
        log.info("Maintenance schedule archived: {}", schedule.getTitle());
    }

    private MaintenanceSchedule findById(Long id) {
        return maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceSchedule not found: " + id));
    }

    private MaintenanceScheduleResponse toResponse(MaintenanceSchedule m) {
        return MaintenanceScheduleResponse.builder()
                .id(m.getId())
                .organizationId(m.getOrganization() != null ? m.getOrganization().getId() : null)
                .organizationName(m.getOrganization() != null ? m.getOrganization().getOrganizationName() : null)
                .title(m.getTitle())
                .description(m.getDescription())
                .reason(m.getReason())
                .startTime(m.getStartTime())
                .endTime(m.getEndTime())
                .actualStartTime(m.getActualStartTime())
                .actualEndTime(m.getActualEndTime())
                .planned(m.getPlanned())
                .notificationSent(m.getNotificationSent())
                .completed(m.getCompleted())
                .active(m.getActive())
                .remarks(m.getRemarks())
                .createdOn(m.getCreatedOn())
                .createdBy(m.getCreatedBy())
                .updatedOn(m.getUpdatedOn())
                .updatedBy(m.getUpdatedBy())
                .build();
    }
}
