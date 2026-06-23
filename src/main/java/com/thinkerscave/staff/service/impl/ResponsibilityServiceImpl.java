package com.thinkerscave.staff.service.impl;

import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.dto.request.ResponsibilityRequest;
import com.thinkerscave.staff.dto.response.ResponsibilityResponse;
import com.thinkerscave.staff.entity.Responsibility;
import com.thinkerscave.staff.repository.ResponsibilityRepository;
import com.thinkerscave.staff.service.ResponsibilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResponsibilityServiceImpl implements ResponsibilityService {

    private final ResponsibilityRepository responsibilityRepository;

    @Override
    @Transactional
    public Long createResponsibility(ResponsibilityRequest request) {
        if (responsibilityRepository.existsByResponsibilityCode(request.getResponsibilityCode())) {
            throw new AlreadyExistsException("Responsibility code already exists: " + request.getResponsibilityCode());
        }
        Responsibility responsibility = new Responsibility();
        mapRequest(request, responsibility);
        Responsibility saved = responsibilityRepository.save(responsibility);
        log.info("Responsibility created: {}", saved.getResponsibilityId());
        return saved.getResponsibilityId();
    }

    @Override
    @Transactional
    public void updateResponsibility(Long id, ResponsibilityRequest request) {
        Responsibility responsibility = getEntity(id);
        if (!responsibility.getResponsibilityCode().equals(request.getResponsibilityCode())
                && responsibilityRepository.existsByResponsibilityCode(request.getResponsibilityCode())) {
            throw new AlreadyExistsException("Responsibility code already exists: " + request.getResponsibilityCode());
        }
        mapRequest(request, responsibility);
        responsibilityRepository.save(responsibility);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponsibilityResponse> getResponsibilityList() {
        return responsibilityRepository.findByActiveTrueOrderByDisplayOrderAscResponsibilityNameAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponsibilityResponse getResponsibilityById(Long id) {
        return toResponse(getEntity(id));
    }

    @Override
    @Transactional
    public void activateResponsibility(Long id) {
        Responsibility r = getEntity(id);
        r.setActive(true);
        responsibilityRepository.save(r);
    }

    @Override
    @Transactional
    public void deactivateResponsibility(Long id) {
        Responsibility r = getEntity(id);
        r.setActive(false);
        responsibilityRepository.save(r);
    }

    private Responsibility getEntity(Long id) {
        return responsibilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Responsibility not found with ID: " + id));
    }

    private void mapRequest(ResponsibilityRequest req, Responsibility r) {
        r.setResponsibilityCode(req.getResponsibilityCode());
        r.setResponsibilityName(req.getResponsibilityName());
        r.setDescription(req.getDescription());
        r.setDisplayOrder(req.getDisplayOrder());
        r.setRemarks(req.getRemarks());
    }

    private ResponsibilityResponse toResponse(Responsibility r) {
        return ResponsibilityResponse.builder()
                .responsibilityId(r.getResponsibilityId())
                .responsibilityCode(r.getResponsibilityCode())
                .responsibilityName(r.getResponsibilityName())
                .description(r.getDescription())
                .displayOrder(r.getDisplayOrder())
                .systemDefined(r.getSystemDefined())
                .active(r.getActive())
                .remarks(r.getRemarks())
                .createdOn(r.getCreatedOn())
                .updatedOn(r.getUpdatedOn())
                .build();
    }
}
