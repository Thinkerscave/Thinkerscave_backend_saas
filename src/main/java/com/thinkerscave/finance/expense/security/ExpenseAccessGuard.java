package com.thinkerscave.finance.expense.security;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.PermissionService;
import com.thinkerscave.shared.context.OrganizationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class ExpenseAccessGuard {
 public static final String EXPENSES="EXPENSES", EXPENSE_HEADS="EXPENSE_HEADS", EXPENSE_PAYMENT="EXPENSE_PAYMENT",
  EXPENSE_APPROVAL="EXPENSE_APPROVAL", EXPENSE_SETTINGS="EXPENSE_SETTINGS";
 private final PermissionService permissionService; private final UserRepository userRepository;
 public void requireView(String r){require(r,"VIEW");} public void requireManage(String r){require(r,"MANAGE");}
 public void requireApprove(String r){require(r,"APPROVE");}
 public boolean canView(String r){return has(r,"VIEW");} public boolean canManage(String r){return has(r,"MANAGE");}
 public boolean canApprove(String r){return has(r,"APPROVE");}
 public String currentUsername(){Authentication a=SecurityContextHolder.getContext().getAuthentication(); return a==null?"system":a.getName();}
 private boolean has(String r,String p){Authentication a=SecurityContextHolder.getContext().getAuthentication();
  if(a==null||a.getName()==null||OrganizationContext.getOrganizationId()==null)return false;
  User u=userRepository.findByUsername(a.getName()).orElse(null);
  return u!=null&&permissionService.hasPermission(u.getId(),OrganizationContext.getOrganizationId(),r,p);}
 private void require(String r,String p){if(!has(r,p))throw new AccessDeniedException(r+":"+p+" required");}
}
