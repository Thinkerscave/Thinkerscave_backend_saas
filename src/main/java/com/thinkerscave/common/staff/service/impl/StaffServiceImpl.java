package com.thinkerscave.common.staff.service.impl;

import com.thinkerscave.common.menum.domain.Role;
import com.thinkerscave.common.menum.repository.RoleRepository;
import com.thinkerscave.common.staff.domain.Branch;
import com.thinkerscave.common.staff.domain.Department;
import com.thinkerscave.common.staff.domain.Staff;
import com.thinkerscave.common.staff.dto.StaffRequestDTO;
import com.thinkerscave.common.staff.dto.StaffResponseDTO;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Map<String, Object> saveOrUpdateStaff(StaffRequestDTO staffRequestDTO) {
        Map<String, Object> data = new HashMap<>();

        try {
            if (staffRequestDTO == null) {
                data.put("isOutcome", false);
                data.put("data", null);
                data.put("message", "Staff payload is required");
                return data;
            }

            Long orgId = com.thinkerscave.common.context.OrganizationContext.getOrganizationId();
            Branch branch = branchRepository.findByBranchCode(staffRequestDTO.getBranchCode())
                    .orElseThrow(() -> new IllegalStateException("Branch not found"));
            Department department = departmentRepository.findByDepartmentCode(staffRequestDTO.getDepartmentCode())
                    .orElseThrow(() -> new IllegalStateException("Department not found"));
            Role userRole = roleRepository.findByRoleCode("TEACHER")
                    .or(() -> roleRepository.findByRoleName("Teacher"))
                    .orElseThrow(() -> new IllegalStateException("Role 'TEACHER' not found"));

            String requestedStaffCode = StringUtils.hasText(staffRequestDTO.getStaffCode())
                    ? staffRequestDTO.getStaffCode().trim()
                    : null;
            Optional<Staff> existingStaff = requestedStaffCode == null
                    ? Optional.empty()
                    : orgId != null
                            ? staffRepository.findByStaffCodeAndOrganizationId(requestedStaffCode, orgId)
                            : staffRepository.findByStaffCode(requestedStaffCode);

            Optional<User> existingUserByEmail = StringUtils.hasText(staffRequestDTO.getEmail())
                    ? userRepository.findByEmailIgnoreCase(staffRequestDTO.getEmail())
                    : Optional.empty();
            if (existingUserByEmail.isPresent()
                    && (existingStaff.isEmpty()
                            || existingStaff.get().getUser() == null
                            || !existingUserByEmail.get().getId().equals(existingStaff.get().getUser().getId()))) {
                data.put("isOutcome", false);
                data.put("message", "Email already exists");
                return data;
            }

            if (existingStaff.isPresent()) {
                Staff presentStaff = existingStaff.get();
                User existingUser = presentStaff.getUser();

                BeanUtils.copyProperties(staffRequestDTO, existingUser, "id", "userCode", "password", "roles", "organizations");
                if (StringUtils.hasText(staffRequestDTO.getUserName())) {
                    existingUser.setUserName(resolveUniqueUserName(staffRequestDTO.getUserName(), existingUser.getId()));
                }
                existingUser = userRepository.save(existingUser);

                BeanUtils.copyProperties(staffRequestDTO, presentStaff, "id", "staffCode", "organizationId", "user", "branch", "department");
                presentStaff.setUser(existingUser);
                presentStaff.setBranch(branch);
                presentStaff.setDepartment(department);

                Staff saved = staffRepository.save(presentStaff);
                data.put("isOutcome", true);
                data.put("message", "Staff Record Updated");
                data.put("data", toResponse(saved));
                return data;
            }

            String resolvedStaffCode = requestedStaffCode != null ? requestedStaffCode : generateStaffCode(staffRequestDTO);
            User newUser = new User();
            BeanUtils.copyProperties(staffRequestDTO, newUser, "id", "userCode", "password", "roles", "organizations");
            newUser.setUserCode(generateUserCode(resolvedStaffCode));
            newUser.setUserName(resolveUniqueUserName(resolveBaseUserName(staffRequestDTO), null));
            newUser.setPassword(passwordEncoder.encode("Password@123"));
            newUser.setRoles(List.of(userRole));
            newUser = userRepository.save(newUser);

            Staff newStaff = new Staff();
            BeanUtils.copyProperties(staffRequestDTO, newStaff, "id", "staffCode", "organizationId", "user", "branch", "department");
            newStaff.setStaffCode(resolvedStaffCode);
            newStaff.setUser(newUser);
            newStaff.setBranch(branch);
            newStaff.setDepartment(department);
            newStaff.setIsActive(true);

            if (orgId != null) {
                newStaff.setOrganizationId(orgId);
            } else {
                log.warn("OrganizationContext is null when saving new Staff — tenant isolation may be broken");
            }

            Staff saved = staffRepository.save(newStaff);
            data.put("isOutcome", true);
            data.put("message", "Staff Record Saved");
            data.put("data", toResponse(saved));
        } catch (Exception e) {
            log.error("Exception occurred while saving/updating staff", e);
            data.put("isOutcome", false);
            data.put("message", "Unexpected error occurred: " + e.getMessage());
        }

        return data;
    }

    @Override
    public Map<String, Object> getAllStaff() {
        Map<String, Object> data = new HashMap<>();
        try {
            // ─── Multi-tenant isolation: only return staff for caller's org ────────
            Long orgId = com.thinkerscave.common.context.OrganizationContext.getOrganizationId();
                List<StaffResponseDTO> staffList = (orgId != null)
                    ? staffRepository.findByOrganizationIdAndIsActive(orgId, true)
                        .stream()
                        .map(this::toResponse)
                        .toList()
                    : List.of(); // Return empty if no org context — never leak cross-org data
            if (!staffList.isEmpty()) {
                data.put("isOutcome", true);
                data.put("message", "All Staff Records Fetched ");
                data.put("data", staffList);
            } else {
                data.put("isOutcome", false);
                data.put("message", orgId == null ? "Organization context not set" : "No active staff found");
            }
        } catch (Exception e) {
            log.error("Exception occurred while Getting staff Details", e);
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
                Long orgId = com.thinkerscave.common.context.OrganizationContext.getOrganizationId();
                // ─── Scope lookup to caller's org — prevents cross-org data access ──
                Staff staff = (orgId != null)
                        ? staffRepository.findByStaffCodeAndOrganizationId(staffCode, orgId)
                                .orElseThrow(() -> new RuntimeException("Staff not found with code: " + staffCode))
                        : staffRepository.findByStaffCode(staffCode)
                                .orElseThrow(() -> new RuntimeException("Staff not found with code: " + staffCode));
                if (staff.getId() != null) {
                    data.put("isOutcome", true);
                    data.put("message", "Staff Records Fetched ");
                    data.put("data", toResponse(staff));
                } else {
                    data.put("isOutcome", false);
                    data.put("message", "Unable to Fetch Staff Record With Code" + staffCode);
                }
            } else {
                data.put("isOutcome", false);
                data.put("message", "Staff Code is Empty");
            }
        } catch (Exception e) {
            log.error("Exception occurred while Getting staff Detail", e);
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
                Staff staff = staffRepository.findByStaffCode(staffCode)
                        .orElseThrow(() -> new RuntimeException("Staff not found with code: " + staffCode));

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
            log.error("Exception occurred in Staff Active Status", e);
            data.put("isOutcome", false);
            data.put("message", "Unexpected error occurred: " + e.getMessage());
        }
        return data;
    }

    private String generateStaffCode(StaffRequestDTO dto) {
        String source = StringUtils.hasText(dto.getEmail()) ? dto.getEmail().toLowerCase() : String.valueOf(System.nanoTime());
        return "STF-" + Integer.toUnsignedString(source.hashCode(), 36).toUpperCase();
    }

    private String generateUserCode(String staffCode) {
        return "USR-" + staffCode.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    private String resolveBaseUserName(StaffRequestDTO dto) {
        if (StringUtils.hasText(dto.getUserName())) {
            return sanitizeUserName(dto.getUserName());
        }
        if (StringUtils.hasText(dto.getEmail()) && dto.getEmail().contains("@")) {
            return sanitizeUserName(dto.getEmail().substring(0, dto.getEmail().indexOf('@')));
        }
        return sanitizeUserName(dto.getFirstName() + "." + dto.getLastName());
    }

    private String resolveUniqueUserName(String requested, Long currentUserId) {
        String base = sanitizeUserName(requested);
        String candidate = base;
        int suffix = 1;
        while (userRepository.findByUserName(candidate)
                .filter(user -> currentUserId == null || !user.getId().equals(currentUserId))
                .isPresent()) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }

    private String sanitizeUserName(String value) {
        String cleaned = value == null ? "staff" : value.trim().toLowerCase().replaceAll("[^a-z0-9._-]", ".");
        cleaned = cleaned.replaceAll("\\.+", ".").replaceAll("^\\.|\\.$", "");
        return StringUtils.hasText(cleaned) ? cleaned : "staff";
    }

    private StaffResponseDTO toResponse(Staff staff) {
        if (staff == null) {
            return null;
        }
        User user = staff.getUser();
        Branch branch = staff.getBranch();
        Department department = staff.getDepartment();

        return StaffResponseDTO.builder()
                .id(staff.getId())
                .staffId(staff.getId())
                .staffCode(staff.getStaffCode())
                .userId(user != null ? user.getId() : null)
                .userName(user != null ? user.getUserName() : null)
                .firstName(staff.getFirstName())
                .middleName(staff.getMiddleName())
                .lastName(staff.getLastName())
                .email(staff.getEmail())
                .mobileNumber(staff.getMobileNumber())
                .gender(staff.getGender())
                .dateOfBirth(staff.getDateOfBirth())
                .hireDate(staff.getHireDate())
                .photoUrl(staff.getPhotoUrl())
                .address(staff.getAddress())
                .city(staff.getCity())
                .state(staff.getState())
                .remarks(staff.getRemarks())
                .isActive(staff.getIsActive())
                .organizationId(staff.getOrganizationId())
                .branchId(branch != null ? branch.getId() : null)
                .branchCode(branch != null ? branch.getBranchCode() : null)
                .branchName(branch != null ? branch.getBranchName() : null)
                .departmentId(department != null ? department.getId() : null)
                .departmentCode(department != null ? department.getDepartmentCode() : null)
                .departmentName(department != null ? department.getDepartmentName() : null)
                .build();
    }

}
