package com.thinkerscave.common.staff.service;

import com.thinkerscave.common.staff.domain.Branch;
import java.util.Map;

public interface BranchService {
    Map<String, Object> getAllActiveBranch();

    Map<String, Object> saveOrUpdate(Branch branch);

    Map<String, Object> toggleActive(Long id);
}
