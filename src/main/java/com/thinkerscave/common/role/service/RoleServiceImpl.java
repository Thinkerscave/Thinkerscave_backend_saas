package com.thinkerscave.common.role.service;


import com.thinkerscave.common.role.DTO.RoleDTO;
import com.thinkerscave.common.role.domain.Role;
import com.thinkerscave.common.role.repository.RoleRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RoleServiceImpl implements RoleService {

	@Autowired
	private RoleRepository repository;

	@Override
	public Role saveData(RoleDTO dto) {
		Role role = new Role();
		BeanUtils.copyProperties(dto, role);

		// Generate Role Code if not already provided
		if (dto.getRoleCode() == null || dto.getRoleCode().isBlank()) {
			// For example, create code like "ROLE_ADMIN_001"
			String baseCode = "ROLE_" + dto.getRoleName().toUpperCase().replaceAll("\\s+", "_");
			String uniqueSuffix = UUID.randomUUID().toString().substring(0, 5).toUpperCase();  // or use a custom ID generator
			role.setRoleCode(baseCode + "_" + uniqueSuffix);
		}

		return repository.save(role);
	}



	@Override
	public List<Role> allRecords() {

		return repository.findAllRoles();
	}

	
	//Soft Deletion
	@Override
	public void delete(Long id) {

		if(id!=null)
		{
			Role role = repository.findById(id).get();
			
			if(role.getIsActive())
			{
				role.setIsActive(false);
				repository.save(role);
			}
		}
		else {
			System.out.println("Id is null");
		}
		
		
	}



	@Override
	public Role editRoleData(Long id) {

		return repository.findById(id).get(); 
	}


	@Override
	public String updateRole(Long id, RoleDTO dto) {
		//Fetch role by id
	   // System.out.println("Institute In Service"+instituteRepo.findById(dto.getInstitute()).get());

		Role existingRole = repository.findById(id)
	            .orElseThrow(() -> new RuntimeException("Role not found"));

	    System.out.println(existingRole);

	    // Update fields
	    existingRole.setRoleName(dto.getRoleName());
	    existingRole.setDescription(dto.getDescription());

	    System.out.println(existingRole);
	    
	    // Save updated role
	    repository.save(existingRole);		
	    
	    return "Role updated successfully";
	}

	
	
}
