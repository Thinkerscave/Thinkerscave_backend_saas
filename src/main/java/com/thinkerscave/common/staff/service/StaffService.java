package com.thinkerscave.common.staff.service;

import com.thinkerscave.common.staff.domain.Staff;
import com.thinkerscave.common.staff.dto.StaffRequestDTO;

import java.util.Map;

public interface StaffService {

    Map<String,Object> saveOrUpdateStaff(StaffRequestDTO staffRequestDTO);
}
