package com.thinkerscave.common.role.service.impl;


import com.thinkerscave.common.role.dto.RoleDTO;
import com.thinkerscave.common.role.domain.Role;
import com.thinkerscave.common.role.repository.RoleRepository;
import com.thinkerscave.common.role.service.RoleService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of RoleService for managing roles.
 *
 * @author Sandeep
 */
@Service
public class RoleServiceImpl implements RoleService {

	@Autowired
	private RoleRepository repository;

	/** Saves or updates a role based on the given code and data. */
	@Override
	public String saveOrUpdateRole(String code, RoleDTO dto) {
		Role role;

		if (code == null || code.isBlank()) {
			// New Role Creation
			role = new Role();
			BeanUtils.copyProperties(dto, role);

			// Generate Role Code if not provided
			if (dto.getRoleCode() == null || dto.getRoleCode().isBlank()) {
				String baseCode = "ROLE_" + dto.getRoleName().toUpperCase().replaceAll("\\s+", "_");
				String uniqueSuffix = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
				role.setRoleCode(baseCode + "_" + uniqueSuffix);
			} else {
				role.setRoleCode(dto.getRoleCode());
			}

		} else {
			// Update existing Role using roleCode
			role = repository.findByRoleCode(code)
					.orElseThrow(() -> new RuntimeException("Role not found with code: " + code));

			// Update fields
			role.setRoleName(dto.getRoleName());
			role.setDescription(dto.getDescription());
			role.setIsActive(dto.getIsActive());
		}

		repository.save(role);

		return (code == null || code.isBlank()) ? "Role created successfully" : "Role updated successfully";
	}

	/** Returns all role records. */
	@Override
	public List<Role> allRecords() {
		return repository.findAllRoles();
	}

	/** Deletes a role by its code. */
	@Override
	public void delete(String code) {
		if (code == null || code.isBlank()) {
			throw new IllegalArgumentException("Role code must not be null or blank for deletion");
		}

		Role role = repository.findByRoleCode(code)
				.orElseThrow(() -> new RuntimeException("Role not found with code: " + code));

		if (Boolean.TRUE.equals(role.getIsActive())) {
			role.setIsActive(false);
			repository.save(role);
		}
	}

	/** Returns role data by code for editing. */
	@Override
	public Role editRoleData(String code) {
		return repository.findByRoleCode(code)
				.orElseThrow(() -> new RuntimeException("Role not found with code: " + code));
	}
}

