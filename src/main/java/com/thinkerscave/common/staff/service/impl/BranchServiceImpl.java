package com.thinkerscave.common.staff.service.impl;

import com.thinkerscave.common.staff.domain.Branch;
import com.thinkerscave.common.staff.domain.Staff;
import com.thinkerscave.common.staff.repository.BranchRepository;
import com.thinkerscave.common.staff.service.BranchService;
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
public class BranchServiceImpl implements BranchService {
    private static final Logger logger = LoggerFactory.getLogger(BranchServiceImpl.class);
    @Autowired
    private BranchRepository branchRepository;

    @Override
    public Map<String, Object> getAllActiveBranch() {
        Map<String, Object> data = new HashMap<>();
        try {
            List<Branch> branchList = branchRepository.findAllByIsActiveTrue();
            if (!branchList.isEmpty()) {
                data.put("isOutcome", true);
                data.put("message", "All Branch Records Fetched ");
                data.put("data", branchList);
            } else {
                data.put("isOutcome", false);
                data.put("message", "Unable to Fetch Branch Records ");
            }

        } catch (Exception e) {
            logger.error("Exception occurred while Getting Branch Details", e);
            data.put("isOutcome", false);
            data.put("message", "Unexpected error occurred: " + e.getMessage());
        }
        return data;
    }
}
