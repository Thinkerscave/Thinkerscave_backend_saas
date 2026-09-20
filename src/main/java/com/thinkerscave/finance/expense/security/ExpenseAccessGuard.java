package com.thinkerscave.finance.expense.security;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.PermissionService;
import com.thinkerscave.shared.context.OrganizationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Privilege checks for Finance Expense APIs.
 * Nested expense workspace resources inherit from {@link #EXPENSES}.
 */
@Component
@RequiredArgsConstructor
public class ExpenseAccessGuard {

    public static final String EXPENSES = "EXPENSES";
    public static final String EXPENSE_HEADS = "EXPENSE_HEADS";
    public static final String EXPENSE_PAYMENT = "EXPENSE_PAYMENT";
    public static final String EXPENSE_APPROVAL = "EXPENSE_APPROVAL";
    public static final String EXPENSE_SETTINGS = "EXPENSE_SETTINGS";

    private static final Set<String> EXPENSE_COVERED = Set.of(
            EXPENSE_HEADS, EXPENSE_PAYMENT, EXPENSE_APPROVAL, EXPENSE_SETTINGS
    );

    private static final Map<String, String> PARENT_RESOURCE = Map.of(
            EXPENSE_HEADS, EXPENSES,
            EXPENSE_PAYMENT, EXPENSES,
            EXPENSE_APPROVAL, EXPENSES,
            EXPENSE_SETTINGS, EXPENSES
    );

    private final PermissionService permissionService;
    private final UserRepository userRepository;

    public void requireView(String r) {
        require(r, "VIEW");
    }

    public void requireManage(String r) {
        require(r, "MANAGE");
    }

    public void requireApprove(String r) {
        require(r, "APPROVE");
    }

    public boolean canView(String r) {
        return has(r, "VIEW");
    }

    public boolean canManage(String r) {
        return has(r, "MANAGE");
    }

    public boolean canApprove(String r) {
        return has(r, "APPROVE");
    }

    public String currentUsername() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "system" : a.getName();
    }

    private boolean has(String r, String p) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getName() == null || OrganizationContext.getOrganizationId() == null) {
            return false;
        }
        User u = userRepository.findByUsername(a.getName()).orElse(null);
        if (u == null) {
            return false;
        }
        Long orgId = OrganizationContext.getOrganizationId();
        if (permissionService.hasPermission(u.getId(), orgId, r, p)) {
            return true;
        }
        String parent = PARENT_RESOURCE.get(r);
        return parent != null && permissionService.hasPermission(u.getId(), orgId, parent, p);
    }

    private void require(String r, String p) {
        if (!has(r, p)) {
            throw new AccessDeniedException(r + ":" + p + " required");
        }
    }

    public static boolean isExpenseCovered(String resource) {
        return EXPENSE_COVERED.contains(resource);
    }
}
