package com.thinkerscave.common.role.service;


import com.thinkerscave.common.role.DTO.RoleDTO;
import com.thinkerscave.common.role.domain.Role;
import com.thinkerscave.common.role.repository.RoleRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

	@Autowired
	private RoleRepository repository;

	@Override
	public Role saveData(RoleDTO dto) {
		
		/*
		 * instituteType institute=new instituteType();
		 * institute.setInstituteTypeName(dto.getInstituteType());
		 */
		
		//instituteRepo.save(institute);

		
		Role role=new Role();

		
		BeanUtils.copyProperties(dto, role);
		
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
