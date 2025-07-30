package com.thinkerscave.common.staff.service.impl;

import com.thinkerscave.common.staff.domain.Branch;
import com.thinkerscave.common.staff.domain.Department;
import com.thinkerscave.common.staff.domain.Staff;
import com.thinkerscave.common.staff.dto.StaffRequestDTO;
import com.thinkerscave.common.staff.repository.BranchRepository;
import com.thinkerscave.common.staff.repository.DepartmentRepository;
import com.thinkerscave.common.staff.repository.StaffRepository;
import com.thinkerscave.common.staff.service.StaffService;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



@Service
public class StaffServiceImpl implements StaffService {

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private DepartmentRepository departmentRepository;
    @Override
    public Map<String,Object> saveOrUpdateStaff(StaffRequestDTO staffRequestDTO) {
        Map<String,Object> data= new HashMap<>();
        Staff staff=null;
        Branch branch=null;
        Department department=null;
        try{
            if(staffRequestDTO!=null){

                branch= branchRepository.findById(staffRequestDTO.getBranchId()).orElse(null);
                department= departmentRepository.findById(staffRequestDTO.getDepartmentId()).orElse(null);

                if(staffRequestDTO.getStaffId()!=null){

                }

            }else {
                data.put("isOutcome",false);
                data.put("data",null);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return data;
    }
}
