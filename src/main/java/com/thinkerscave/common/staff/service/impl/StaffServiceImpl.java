package com.thinkerscave.common.staff.service.impl;

import com.thinkerscave.common.menum.domain.Role;
import com.thinkerscave.common.menum.repository.RoleRepository;
import com.thinkerscave.common.staff.domain.Branch;
import com.thinkerscave.common.staff.domain.Department;
import com.thinkerscave.common.staff.domain.Staff;
import com.thinkerscave.common.staff.dto.StaffRequestDTO;
import com.thinkerscave.common.staff.repository.BranchRepository;
import com.thinkerscave.common.staff.repository.DepartmentRepository;
import com.thinkerscave.common.staff.repository.StaffRepository;
import com.thinkerscave.common.staff.service.StaffService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class StaffServiceImpl implements StaffService {

    private static final Logger logger = LoggerFactory.getLogger(StaffServiceImpl.class);

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RoleRepository roleRepository;


    @Override
    public Map<String, Object> saveOrUpdateStaff(StaffRequestDTO staffRequestDTO) {
        Map<String, Object> data = new HashMap<>();

        Branch branch = null;
        Department department = null;
        Role userRole = null;

        try {
            if (staffRequestDTO != null) {

                // Check email uniqueness
                Optional<User> existingUserByEmail = userRepository.findByEmailIgnoreCase(staffRequestDTO.getEmail());

                // If email belongs to another user
                if (existingUserByEmail.isPresent() && existingUserByEmail.get().getEmail().equalsIgnoreCase(staffRequestDTO.getEmail())) {
                    data.put("isOutcome", false);
                    data.put("message", "Email already Exist");
                    return data;
                }

                userRole = roleRepository.findByRoleName("USER").orElseThrow(() -> new IllegalStateException("Role 'USER' not found"));
                branch = branchRepository.findByBranchCode(staffRequestDTO.getBranchCode()).orElseThrow(() -> new IllegalStateException("Branch not found"));
                department = departmentRepository.findByDepartmentCode(staffRequestDTO.getDepartmentCode()).orElseThrow(() -> new IllegalStateException("Department not found"));


                if (staffRequestDTO.getStaffCode() != null) {
                    // Update case
                    Staff presentStaff = staffRepository.findByStaffCode(staffRequestDTO.getStaffCode()).orElseThrow(() -> new IllegalStateException("Current Staff not found with code"+staffRequestDTO.getStaffCode()));

                    User existingUser = presentStaff.getUser();

                        // Update the Existing User
                        BeanUtils.copyProperties(staffRequestDTO, existingUser, "userName", "id", "userCode");
                        existingUser = userRepository.save(existingUser);

                        // Update the Existing Staff
                        BeanUtils.copyProperties(staffRequestDTO, presentStaff, "hireDate", "id", "staffCode");
                        presentStaff.setUser(existingUser);
                        presentStaff.setBranch(branch);
                        presentStaff.setDepartment(department);

                        presentStaff = staffRepository.save(presentStaff);
                        if (presentStaff.getId() != null) {
                            data.put("isOutcome", true);
                            data.put("message", "Staff Record Updated ");
                            data.put("data", presentStaff);
                        } else {
                            data.put("isOutcome", false);
                            data.put("message", "Unable To Update Staff Record ");
                        }


                } else {
                    User newUser = new User();
                    Staff newStaff = new Staff();

                    // Saving the New User
                    BeanUtils.copyProperties(staffRequestDTO, newUser, "id");
                    newUser.setRoles(List.of(userRole));
                    // Here set the User(Code,Username,Password)

                    newUser = userRepository.save(newUser);

                    BeanUtils.copyProperties(staffRequestDTO, newStaff, "id");
                    // Here set the Staff Code
                    newStaff.setUser(newUser);
                    newStaff.setBranch(branch);
                    newStaff.setDepartment(department);

                    newStaff = staffRepository.save(newStaff);

                    if (newStaff.getId() != null) {
                        data.put("isOutcome", true);
                        data.put("message", "Staff Record Saved ");
                        data.put("data", newStaff);
                    } else {
                        data.put("isOutcome", false);
                        data.put("message", "Unable To Save Staff Record ");
                    }
                }

            } else {
                data.put("isOutcome", false);
                data.put("data", null);
            }
        } catch (Exception e) {
            logger.error("Exception occurred while saving/updating staff", e);
            data.put("isOutcome", false);
            data.put("message", "Unexpected error occurred: " + e.getMessage());
        }

        return data;
    }

    @Override
    public Map<String, Object> getAllStaff() {
        Map<String, Object> data = new HashMap<>();
        try {
            List<Staff> staffList = staffRepository.findAll();
            if (!staffList.isEmpty()) {
                data.put("isOutcome", true);
                data.put("message", "All Staff Records Fetched ");
                data.put("data", staffList);
            } else {
                data.put("isOutcome", false);
                data.put("message", "Unable to Fetch Staff Records ");
            }

        } catch (Exception e) {
            logger.error("Exception occurred while Getting staff Details", e);
            data.put("isOutcome", false);
            data.put("message", "Unexpected error occurred: " + e.getMessage());
        }
        return data;
    }

    @Override
    public Map<String, Object> getByStaffCode(String staffCode) {
        Map<String, Object> data = new HashMap<>();
        try {
            if (!staffCode.isEmpty()) {
                Staff staff = staffRepository.findByStaffCode(staffCode).orElseThrow(() -> new RuntimeException("Staff not found with code: " + staffCode));
                if (staff.getId() != null) {
                    data.put("isOutcome", true);
                    data.put("message", "Staff Records Fetched ");
                    data.put("data", staff);
                } else {
                    data.put("isOutcome", false);
                    data.put("message", "Unable to Fetch Staff Record With Code" + staffCode);
                }
            } else {
                data.put("isOutcome", false);
                data.put("message", "Staff Code is Empty");
            }


        } catch (Exception e) {
            logger.error("Exception occurred while Getting staff Detail", e);
            data.put("isOutcome", false);
            data.put("message", "Unexpected error occurred: " + e.getMessage());
        }
        return data;
    }

    @Override
    public Map<String, Object> staffActiveStatus(String staffCode) {
        Map<String, Object> data = new HashMap<>();
        try {
            if (!staffCode.isEmpty()) {
                Staff staff = staffRepository.findByStaffCode(staffCode).orElseThrow(() -> new RuntimeException("Staff not found with code: " + staffCode));

                    staff.setIsActive(!staff.getIsActive());
                    staff = staffRepository.save(staff);
                    if (staff.getId() != null) {
                        data.put("isOutcome", true);
                        data.put("message", "Staff Record " + (staff.getIsActive() ? "Activated" : "Deactivated"));

                    } else {
                        data.put("isOutcome", false);
                        data.put("message", "Unable to set Staff Active Status");

                    }

            } else {
                data.put("isOutcome", false);
                data.put("message", "Staff Code is null ");

            }

        } catch (Exception e) {
            logger.error("Exception occurred in Staff Active Status", e);
            data.put("isOutcome", false);
            data.put("message", "Unexpected error occurred: " + e.getMessage());
        }
        return data;
    }


}
