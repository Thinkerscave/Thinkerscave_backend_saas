package com.thinkerscave.common.staff.service.impl;

import org.springframework.transaction.annotation.Transactional;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.staff.domain.Branch;
import com.thinkerscave.common.staff.repository.BranchRepository;
import com.thinkerscave.common.staff.service.BranchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;

    @Override
    public Map<String, Object> getAllActiveBranch() {
        Map<String, Object> data = new HashMap<>();
        try {
            Long orgId = OrganizationContext.getOrganizationId();
            List<Branch> branchList = (orgId != null)
                    ? branchRepository.findByOrganizationIdAndIsActive(orgId, true)
                    : branchRepository.findAllByIsActiveTrue();
            if (!branchList.isEmpty()) {
                data.put("isOutcome", true);
                data.put("message", "All Branch Records Fetched");
                data.put("data", branchList);
            } else {
                data.put("isOutcome", false);
                data.put("message", "No active branches found");
            }
        } catch (Exception e) {
            log.error("Exception while fetching branches", e);
            data.put("isOutcome", false);
            data.put("message", "Unexpected error: " + e.getMessage());
        }
        return data;
    }

    @Override
    public Map<String, Object> saveOrUpdate(Branch branch) {
        Map<String, Object> data = new HashMap<>();
        try {
            // Stamp org context for new branches
            if (branch.getId() == null) {
                Long orgId = OrganizationContext.getOrganizationId();
                if (orgId != null)
                    branch.setOrganizationId(orgId);
                if (branch.getIsActive() == null)
                    branch.setIsActive(true);
            }
            Branch saved = branchRepository.save(branch);
            data.put("isOutcome", true);
            data.put("message", branch.getId() == null ? "Branch created" : "Branch updated");
            data.put("data", saved);
        } catch (Exception e) {
            log.error("Exception while saving branch", e);
            data.put("isOutcome", false);
            data.put("message", "Unexpected error: " + e.getMessage());
        }
        return data;
    }

    @Override
    public Map<String, Object> toggleActive(Long id) {
        Map<String, Object> data = new HashMap<>();
        try {
            Branch branch = branchRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Branch not found: " + id));
            branch.setIsActive(!branch.getIsActive());
            branchRepository.save(branch);
            data.put("isOutcome", true);
            data.put("message", "Branch " + (branch.getIsActive() ? "activated" : "deactivated"));
        } catch (Exception e) {
            log.error("Exception while toggling branch status", e);
            data.put("isOutcome", false);
            data.put("message", "Unexpected error: " + e.getMessage());
        }
        return data;
    }
}
