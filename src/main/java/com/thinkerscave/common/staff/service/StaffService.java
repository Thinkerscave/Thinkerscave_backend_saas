package com.thinkerscave.common.staff.service;

import com.thinkerscave.common.staff.domain.Staff;
import com.thinkerscave.common.staff.dto.StaffRequestDTO;

import java.util.Map;

public interface StaffService {

    Map<String,Object> saveOrUpdateStaff(StaffRequestDTO staffRequestDTO);

    Map<String,Object> getAllStaff();

    Map<String,Object> getByStaffCode(String staffCode);

    Map<String,Object> staffActiveStatus(String staffCode);

}
