package com.thinkerscave.finance.payroll.security;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.PermissionService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Privilege checks for Finance Payroll APIs. No elevated-role bypass.
 * Privileges: VIEW / MANAGE / APPROVE only (no PAY).
 */
@Component
@RequiredArgsConstructor
public class PayrollAccessGuard {

    public static final String RESOURCE_PAYROLL = "PAYROLL";
    public static final String RESOURCE_COMPONENTS = "PAYROLL_COMPONENTS";
    public static final String RESOURCE_STRUCTURES = "PAYROLL_STRUCTURES";
    public static final String RESOURCE_EMPLOYEE_SALARY = "PAYROLL_EMPLOYEE_SALARY";
    public static final String RESOURCE_RUN = "PAYROLL_RUN";
    public static final String RESOURCE_PAYMENT = "PAYROLL_PAYMENT";
    public static final String RESOURCE_PAYSLIP = "PAYROLL_PAYSLIP";
    public static final String RESOURCE_SETTINGS = "PAYROLL_SETTINGS";
    public static final String RESOURCE_MY = "PAYROLL_MY";

    private final PermissionService permissionService;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;

    public void requireView(String resource) {
        require(resource, "VIEW");
    }

    public void requireManage(String resource) {
        require(resource, "MANAGE");
    }

    public void requireApprove(String resource) {
        require(resource, "APPROVE");
    }

    public boolean canView(String resource) {
        return hasPermission(resource, "VIEW");
    }

    public boolean canManage(String resource) {
        return hasPermission(resource, "MANAGE");
    }

    public Long currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).map(User::getId).orElse(null);
    }

    public String currentUsernameOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    public Staff requireLinkedStaff() {
        Long userId = currentUserIdOrNull();
        if (userId == null) {
            throw new AccessDeniedException("Authenticated user required");
        }
        return staffRepository.findByUser_Id(userId)
                .orElseThrow(() -> new BadRequestException("No staff profile linked to current user"));
    }

    public void assertSelfStaff(Long staffId) {
        Staff self = requireLinkedStaff();
        if (staffId == null || !self.getStaffId().equals(staffId)) {
            throw new AccessDeniedException("PAYROLL_MY: self scope only");
        }
    }

    private boolean hasPermission(String resource, String privilege) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return false;
        }
        Long orgId = OrganizationContext.getOrganizationId();
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || orgId == null) {
            return false;
        }
        return permissionService.hasPermission(user.getId(), orgId, resource, privilege);
    }

    private void require(String resource, String privilege) {
        if (!hasPermission(resource, privilege)) {
            throw new AccessDeniedException(resource + ":" + privilege + " required");
        }
    }
}
