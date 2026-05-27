package com.thinkerscave.common.rbac.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.enums.GenericStatus;
import com.thinkerscave.common.exception.BadRequestException;
import com.thinkerscave.common.exception.ConflictException;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.rbac.domain.Responsibility;
import com.thinkerscave.common.rbac.domain.ResponsibilityPrivilege;
import com.thinkerscave.common.rbac.domain.UserResponsibility;
import com.thinkerscave.common.rbac.dto.ResponsibilityDTO;
import com.thinkerscave.common.rbac.dto.UserResponsibilityDTO;
import com.thinkerscave.common.rbac.repository.ResponsibilityPrivilegeRepository;
import com.thinkerscave.common.rbac.repository.ResponsibilityRepository;
import com.thinkerscave.common.rbac.repository.UserResponsibilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Responsibility CRUD plus privilege mapping and user assignment.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ResponsibilityService {

    private final ResponsibilityRepository responsibilityRepository;
    private final ResponsibilityPrivilegeRepository privilegeRepository;
    private final UserResponsibilityRepository userResponsibilityRepository;
    private final AuditPublisher auditPublisher;

    public List<ResponsibilityDTO> list() {
        return responsibilityRepository.findByOrganizationId(currentOrgId())
                .stream().map(this::toDtoWithPrivileges).toList();
    }

    public ResponsibilityDTO get(Long id) {
        return toDtoWithPrivileges(load(id));
    }

    @Transactional
    public ResponsibilityDTO save(ResponsibilityDTO dto) {
        if (dto.getCode() == null || dto.getCode().isBlank()) {
            throw new BadRequestException("code is required");
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BadRequestException("name is required");
        }
        Long orgId = currentOrgId();
        Responsibility r;
        if (dto.getId() != null) {
            r = load(dto.getId());
            if (!r.getCode().equals(dto.getCode())) {
                final Long currentId = r.getId();
                responsibilityRepository.findByOrganizationIdAndCode(orgId, dto.getCode())
                        .ifPresent(other -> {
                            if (!other.getId().equals(currentId)) {
                                throw new ConflictException("Responsibility code already exists: " + dto.getCode());
                            }
                        });
                r.setCode(dto.getCode());
            }
        } else {
            responsibilityRepository.findByOrganizationIdAndCode(orgId, dto.getCode())
                    .ifPresent(x -> { throw new ConflictException("Responsibility code already exists: " + dto.getCode()); });
            r = Responsibility.builder().code(dto.getCode()).build();
            r.setOrganizationId(orgId);
        }
        r.setName(dto.getName());
        r.setDescription(dto.getDescription());
        r.setScopeType(dto.getScopeType());
        r.setStatus(dto.getStatus() != null ? dto.getStatus() : GenericStatus.ACTIVE);
        boolean creating = r.getId() == null;
        r = responsibilityRepository.save(r);

        if (dto.getPrivilegeIds() != null) {
            privilegeRepository.deleteByResponsibilityId(r.getId());
            for (Long privId : dto.getPrivilegeIds()) {
                if (privId == null) continue;
                privilegeRepository.save(ResponsibilityPrivilege.builder()
                        .responsibilityId(r.getId())
                        .privilegeId(privId)
                        .build());
            }
        }
        auditPublisher.publish(creating ? AuditEventType.CREATE : AuditEventType.UPDATE,
                creating ? "RESPONSIBILITY_CREATE" : "RESPONSIBILITY_UPDATE",
                "Responsibility", r.getId(), "Responsibility " + r.getCode() + (creating ? " created" : " updated"));
        return toDtoWithPrivileges(r);
    }

    @Transactional
    public void delete(Long id) {
        Responsibility r = load(id);
        if (!userResponsibilityRepository.findByResponsibilityIdAndActive(id, true).isEmpty()) {
            throw new ConflictException("Cannot delete responsibility with active assignments");
        }
        privilegeRepository.deleteByResponsibilityId(id);
        responsibilityRepository.delete(r);
        auditPublisher.publish(AuditEventType.DELETE, "RESPONSIBILITY_DELETE", "Responsibility",
                id, "Responsibility " + r.getCode() + " deleted");
    }

    // ---- user assignment --------------------------------------------------

    public List<UserResponsibilityDTO> listForUser(Long userId) {
        return userResponsibilityRepository
                .findByUserIdAndOrganizationIdAndActive(userId, currentOrgId(), true)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public UserResponsibilityDTO assign(UserResponsibilityDTO dto) {
        if (dto.getUserId() == null || dto.getResponsibilityId() == null) {
            throw new BadRequestException("userId and responsibilityId are required");
        }
        load(dto.getResponsibilityId()); // validates org scope
        Long orgId = currentOrgId();
        UserResponsibility ur = UserResponsibility.builder()
                .userId(dto.getUserId())
                .responsibilityId(dto.getResponsibilityId())
                .scopeRefId(dto.getScopeRefId())
                .academicYearId(dto.getAcademicYearId())
                .validFrom(dto.getValidFrom() != null ? dto.getValidFrom() : LocalDate.now())
                .validTo(dto.getValidTo())
                .active(true)
                .build();
        ur.setOrganizationId(orgId);
        ur = userResponsibilityRepository.save(ur);
        auditPublisher.publish(AuditEventType.CREATE, "USER_RESPONSIBILITY_ASSIGN", "UserResponsibility",
                ur.getId(), "User " + dto.getUserId() + " assigned responsibility " + dto.getResponsibilityId());
        return toDto(ur);
    }

    @Transactional
    public void revoke(Long userResponsibilityId) {
        UserResponsibility ur = userResponsibilityRepository.findById(userResponsibilityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User responsibility not found: " + userResponsibilityId));
        Long orgId = currentOrgId();
        if (orgId != null && !orgId.equals(ur.getOrganizationId())) {
            throw new ResourceNotFoundException("User responsibility not found: " + userResponsibilityId);
        }
        ur.setActive(false);
        ur.setValidTo(LocalDate.now());
        userResponsibilityRepository.save(ur);
        auditPublisher.publish(AuditEventType.DELETE, "USER_RESPONSIBILITY_REVOKE", "UserResponsibility",
                ur.getId(), "User responsibility " + ur.getId() + " revoked");
    }

    // ---- helpers ----------------------------------------------------------

    private Responsibility load(Long id) {
        Responsibility r = responsibilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Responsibility not found: " + id));
        Long orgId = currentOrgId();
        if (orgId != null && !orgId.equals(r.getOrganizationId())) {
            throw new ResourceNotFoundException("Responsibility not found: " + id);
        }
        return r;
    }

    private ResponsibilityDTO toDtoWithPrivileges(Responsibility r) {
        List<Long> privIds = privilegeRepository.findByResponsibilityId(r.getId())
                .stream().map(ResponsibilityPrivilege::getPrivilegeId).toList();
        return ResponsibilityDTO.builder()
                .id(r.getId())
                .code(r.getCode())
                .name(r.getName())
                .description(r.getDescription())
                .scopeType(r.getScopeType())
                .status(r.getStatus())
                .privilegeIds(privIds)
                .build();
    }

    private UserResponsibilityDTO toDto(UserResponsibility ur) {
        return UserResponsibilityDTO.builder()
                .id(ur.getId())
                .userId(ur.getUserId())
                .responsibilityId(ur.getResponsibilityId())
                .scopeRefId(ur.getScopeRefId())
                .academicYearId(ur.getAcademicYearId())
                .validFrom(ur.getValidFrom())
                .validTo(ur.getValidTo())
                .active(ur.isActive())
                .build();
    }

    private Long currentOrgId() {
        return OrganizationContext.getOrganizationId();
    }
}
