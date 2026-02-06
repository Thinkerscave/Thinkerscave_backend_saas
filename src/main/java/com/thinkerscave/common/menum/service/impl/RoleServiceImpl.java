package com.thinkerscave.common.menum.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.thinkerscave.common.menum.domain.Role;
import com.thinkerscave.common.menum.dto.RoleDTO;
import com.thinkerscave.common.menum.dto.RoleLookupDTO;
import com.thinkerscave.common.menum.repository.RoleRepository;
import com.thinkerscave.common.menum.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public RoleDTO saveOrUpdateRole(RoleDTO dto) {
        log.info("Saving or updating role: {}", dto);

        Role role;
        if (dto.getRoleId() != null) {
            // update
            role = roleRepository.findById(dto.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role not found with id: " + dto.getRoleId()));
        } else {
            role = new Role();
        }

        role.setRoleName(dto.getRoleName());
        role.setRoleCode(dto.getRoleName());
        role.setDescription(dto.getDescription());
        role.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        role.setRoleType(dto.getRoleType());
        role.setOrganizationId(dto.getOrganizationId());

        Role saved = roleRepository.save(role);
        log.info("Role saved successfully with id: {}", saved.getRoleId());

        return mapToDTO(saved);
    }

    @Override
    public List<RoleDTO> getAllRoles() {
        log.info("Fetching all roles");
        return roleRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public RoleDTO getRoleByCode(String roleCode) {
        log.info("Fetching role by code: {}", roleCode);
        Role role = roleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new RuntimeException("Role not found with code: " + roleCode));
        return mapToDTO(role);
    }

    @Override
    @Transactional
    public void updateRoleStatus(Long roleId, Boolean status) {
        log.info("Updating role status - roleId: {}, roleCode: {}, status: {}", roleId, status);

        Role role;
        if (roleId != null) {
            role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));
        } else {
            throw new IllegalArgumentException("Either roleId or roleCode must be provided");
        }

        role.setIsActive(status);
        roleRepository.save(role);

        log.info("Role status updated successfully: {}", role.getRoleId());

    }

    private RoleDTO mapToDTO(Role role) {
        RoleDTO dto = new RoleDTO();
        dto.setRoleId(role.getRoleId());
        dto.setRoleName(role.getRoleName());
        dto.setRoleCode(role.getRoleCode());
        dto.setDescription(role.getDescription());
        dto.setIsActive(role.getIsActive());
        dto.setRoleType(role.getRoleType());
        dto.setOrganizationId(role.getOrganizationId());
        dto.setCreatedBy(role.getCreatedBy());
        dto.setLastModifiedDate(role.getLastModifiedDate());
        return dto;
    }

    @Override
    public List<RoleLookupDTO> getActiveRoles() {
        log.info("Fetching active roles for dropdown");
        return roleRepository.findByIsActiveTrue()
                .stream()
                .map(role -> new RoleLookupDTO(
                        role.getRoleId(),
                        role.getRoleName(),
                        role.getRoleCode()))
                .collect(Collectors.toList());
    }

}
