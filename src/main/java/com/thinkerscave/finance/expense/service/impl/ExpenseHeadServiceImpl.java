package com.thinkerscave.finance.expense.service.impl;
import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.expense.dto.ExpenseDtos.*;
import com.thinkerscave.finance.expense.entity.*;
import com.thinkerscave.finance.expense.enums.ExpenseHeadStatus;
import com.thinkerscave.finance.expense.repository.*;
import com.thinkerscave.finance.expense.security.ExpenseAccessGuard;
import com.thinkerscave.finance.expense.service.ExpenseHeadService;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.exceptions.*;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ExpenseHeadServiceImpl implements ExpenseHeadService {
 private final ExpenseHeadRepository repository;private final ExpenseRepository expenses;private final ExpenseCategoryRepository categories;
 private final StaffRepository staff;private final ExpenseAccessGuard guard;private final AuditWriteService audit;
 public PageResponse<HeadResponse> list(String q,Pageable p){guard.requireView(ExpenseAccessGuard.EXPENSE_HEADS);Specification<ExpenseHead>s=(r,x,b)->q==null||q.isBlank()?b.conjunction():b.like(b.lower(r.get("name")),"%"+q.toLowerCase()+"%");return PageResponse.of(repository.findAll(s,p),this::map);}
 public List<HeadResponse> lookups(){if(!guard.canView(ExpenseAccessGuard.EXPENSE_HEADS)&&!guard.canView(ExpenseAccessGuard.EXPENSES)&&!guard.canManage(ExpenseAccessGuard.EXPENSES))guard.requireView(ExpenseAccessGuard.EXPENSE_HEADS);return repository.findByStatusOrderByNameAsc(ExpenseHeadStatus.ACTIVE).stream().map(this::map).toList();}
 public HeadResponse get(Long id){guard.requireView(ExpenseAccessGuard.EXPENSE_HEADS);return map(require(id));}
 @Transactional public HeadResponse create(HeadRequest r){guard.requireManage(ExpenseAccessGuard.EXPENSE_HEADS);if(repository.existsByNameIgnoreCase(r.name()))throw new BadRequestException("Expense head name already exists");ExpenseHead h=new ExpenseHead();apply(h,r);h=repository.save(h);log("EXPENSE_HEAD_CREATE",h.getExpenseHeadId());return map(h);}
 @Transactional public HeadResponse update(Long id,HeadRequest r){guard.requireManage(ExpenseAccessGuard.EXPENSE_HEADS);if(repository.existsByNameIgnoreCaseAndExpenseHeadIdNot(r.name(),id))throw new BadRequestException("Expense head name already exists");ExpenseHead h=require(id);apply(h,r);repository.save(h);log("EXPENSE_HEAD_UPDATE",id);return map(h);}
 @Transactional public HeadResponse updateStatus(Long id,HeadStatusRequest r){guard.requireManage(ExpenseAccessGuard.EXPENSE_HEADS);ExpenseHead h=require(id);h.setStatus(r.status());repository.save(h);log("EXPENSE_HEAD_STATUS",id);return map(h);}
 @Transactional public void delete(Long id){guard.requireManage(ExpenseAccessGuard.EXPENSE_HEADS);if(expenses.countByExpenseHead_ExpenseHeadId(id)>0)throw new BadRequestException("Expense head is referenced; deactivate it instead");repository.delete(require(id));log("EXPENSE_HEAD_DELETE",id);}
 private ExpenseHead require(Long id){return repository.findById(id).orElseThrow(()->new ResourceNotFoundException("Expense head not found: "+id));}
 private void apply(ExpenseHead h,HeadRequest r){h.setName(r.name().trim());h.setCategory(categories.findById(r.expenseCategoryId()).orElseThrow(()->new ResourceNotFoundException("Expense category not found")));h.setDefaultRequesterStaffId(r.defaultRequesterStaffId()==null?null:activeStaff(r.defaultRequesterStaffId()).getStaffId());h.setDescription(r.description());h.setStatus(r.status());}
 private Staff activeStaff(Long id){Staff s=staff.findById(id).orElseThrow(()->new ResourceNotFoundException("Staff not found"));if(!Boolean.TRUE.equals(s.getActive()))throw new BadRequestException("Staff is inactive");return s;}
 private HeadResponse map(ExpenseHead h){ExpenseCategory c=h.getCategory();Staff s=h.getDefaultRequesterStaffId()==null?null:staff.findById(h.getDefaultRequesterStaffId()).orElse(null);return new HeadResponse(h.getExpenseHeadId(),h.getName(),new CategoryResponse(c.getExpenseCategoryId(),c.getCode(),c.getName(),c.getDescription(),c.getSortOrder()),h.getDefaultRequesterStaffId(),s==null?null:s.getFirstName()+" "+s.getLastName(),h.getDescription(),h.getStatus());}
 private void log(String action,Long id){audit.record(AuditEventType.UPDATE,action,"ExpenseHead",String.valueOf(id),action);}
}
