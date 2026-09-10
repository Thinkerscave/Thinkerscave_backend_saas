package com.thinkerscave.staff.service.impl;

import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.BadRequestException;
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
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResponsibilityServiceImpl implements ResponsibilityService {

    /** Protected system codes — never creatable, renamable, or deletable via API. */
    private static final Set<String> PROTECTED_SYSTEM_CODES = Set.of("COUNSELOR");

    private final ResponsibilityRepository responsibilityRepository;

    @Override
    @Transactional
    public Long createResponsibility(ResponsibilityRequest request) {
        String code = normalizeCode(request.getResponsibilityCode());
        if (PROTECTED_SYSTEM_CODES.contains(code)) {
            throw new BadRequestException("Responsibility code '" + code + "' is system-defined and cannot be created manually");
        }
        if (responsibilityRepository.existsByResponsibilityCode(code)) {
            throw new AlreadyExistsException("Responsibility code already exists: " + code);
        }
        Responsibility responsibility = new Responsibility();
        mapRequest(request, responsibility);
        responsibility.setResponsibilityCode(code);
        responsibility.setSystemDefined(false);
        responsibility.setOrganizationEditable(true);
        responsibility.setActive(true);
        if (responsibility.getDisplayOrder() == null) {
            responsibility.setDisplayOrder(0);
        }
        Responsibility saved = responsibilityRepository.save(responsibility);
        log.info("Responsibility created: {}", saved.getResponsibilityId());
        return saved.getResponsibilityId();
    }

    @Override
    @Transactional
    public void updateResponsibility(Long id, ResponsibilityRequest request) {
        Responsibility responsibility = getEntity(id);
        boolean definitionLocked = isDefinitionLocked(responsibility);

        if (definitionLocked) {
            // Non-renamable / non-recodeable: keep identity fields immutable.
            request.setResponsibilityCode(responsibility.getResponsibilityCode());
            request.setResponsibilityName(responsibility.getResponsibilityName());
        } else {
            String nextCode = normalizeCode(request.getResponsibilityCode());
            if (!responsibility.getResponsibilityCode().equalsIgnoreCase(nextCode)
                    && responsibilityRepository.existsByResponsibilityCode(nextCode)) {
                throw new AlreadyExistsException("Responsibility code already exists: " + nextCode);
            }
            if (PROTECTED_SYSTEM_CODES.contains(nextCode)) {
                throw new BadRequestException("Responsibility code '" + nextCode + "' is reserved for a system responsibility");
            }
            request.setResponsibilityCode(nextCode);
        }

        mapRequest(request, responsibility);
        // Never allow clients to flip protection flags through the update DTO.
        responsibilityRepository.save(responsibility);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponsibilityResponse> getResponsibilityList(boolean includeInactive) {
        List<Responsibility> rows = includeInactive
                ? responsibilityRepository.findAllByOrderByCreatedOnDesc()
                : responsibilityRepository.findByActiveTrueOrderByDisplayOrderAscResponsibilityNameAsc();
        return rows.stream()
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
        if (isDefinitionLocked(r)) {
            throw new BadRequestException(
                    "System responsibility '" + r.getResponsibilityCode() + "' cannot be deactivated");
        }
        r.setActive(false);
        responsibilityRepository.save(r);
    }

    private boolean isDefinitionLocked(Responsibility r) {
        if (r == null) {
            return false;
        }
        if (PROTECTED_SYSTEM_CODES.contains(normalizeCode(r.getResponsibilityCode()))) {
            return true;
        }
        if (Boolean.TRUE.equals(r.getSystemDefined())) {
            return true;
        }
        return Boolean.FALSE.equals(r.getOrganizationEditable());
    }

    private String normalizeCode(String code) {
        return StringUtils.hasText(code) ? code.trim().toUpperCase(Locale.ROOT) : "";
    }

    private Responsibility getEntity(Long id) {
        return responsibilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Responsibility not found with ID: " + id));
    }

    private void mapRequest(ResponsibilityRequest req, Responsibility r) {
        r.setResponsibilityCode(req.getResponsibilityCode());
        r.setResponsibilityName(req.getResponsibilityName());
        r.setDescription(req.getDescription());
        if (req.getDisplayOrder() != null) {
            r.setDisplayOrder(req.getDisplayOrder());
        }
        r.setRemarks(req.getRemarks());
    }

    private ResponsibilityResponse toResponse(Responsibility r) {
        boolean locked = isDefinitionLocked(r);
        return ResponsibilityResponse.builder()
                .responsibilityId(r.getResponsibilityId())
                .responsibilityCode(r.getResponsibilityCode())
                .responsibilityName(r.getResponsibilityName())
                .description(r.getDescription())
                .displayOrder(r.getDisplayOrder())
                .systemDefined(Boolean.TRUE.equals(r.getSystemDefined()))
                .organizationEditable(Boolean.TRUE.equals(r.getOrganizationEditable()))
                .definitionLocked(locked)
                .active(r.getActive())
                .remarks(r.getRemarks())
                .createdOn(r.getCreatedOn())
                .updatedOn(r.getUpdatedOn())
                .build();
    }
}
