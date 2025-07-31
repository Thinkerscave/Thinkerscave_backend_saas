package com.thinkerscave.common.staff.service.impl;

import com.thinkerscave.common.staff.domain.Branch;
import com.thinkerscave.common.staff.domain.Department;
import com.thinkerscave.common.staff.repository.DepartmentRepository;
import com.thinkerscave.common.staff.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {
    @Autowired
    private DepartmentRepository departmentRepository;
    private static final Logger logger = LoggerFactory.getLogger(DepartmentServiceImpl.class);

    @Override
    public Map<String, Object> getAllActiveDepartment() {
        Map<String, Object> data = new HashMap<>();
        try {
            List<Department> departmentList = departmentRepository.findAllByIsActiveTrue();
            if (!departmentList.isEmpty()) {
                data.put("isOutcome", true);
                data.put("message", "All Department Records Fetched ");
                data.put("data", departmentList);
            } else {
                data.put("isOutcome", false);
                data.put("message", "Unable to Fetch Department Records ");
            }

        } catch (Exception e) {
            logger.error("Exception occurred while Getting Department Details", e);
            data.put("isOutcome", false);
            data.put("message", "Unexpected error occurred: " + e.getMessage());
        }
        return data;
    }
}
