package com.thinkerscave.finance.expense.service.impl;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.finance.expense.dto.ExpenseDtos.*;
import com.thinkerscave.finance.expense.entity.Expense;
import com.thinkerscave.finance.expense.enums.*;
import com.thinkerscave.finance.expense.repository.ExpenseRepository;
import com.thinkerscave.finance.expense.security.ExpenseAccessGuard;
import com.thinkerscave.finance.expense.service.ExpenseOverviewService;
import com.thinkerscave.shared.dto.PageResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ExpenseOverviewServiceImpl implements ExpenseOverviewService {
 private final ExpenseRepository repository;private final AcademicYearRepository academicYears;private final ExpenseAccessGuard guard;
 public PageResponse<ListRow> list(Filter f,Pageable p){guard.requireView(ExpenseAccessGuard.EXPENSES);Range d=range(f);return PageResponse.of(repository.findAll(spec(f,d),p),this::row);}
 public OverviewResponse overview(Filter f){guard.requireView(ExpenseAccessGuard.EXPENSES);Range d=range(f);List<Expense> all=repository.findAll(spec(f,d));
  List<Expense> pending=all.stream().filter(e->e.getApprovalStatus()==ExpenseApprovalStatus.PENDING_APPROVAL).toList();
  List<Expense> unpaid=all.stream().filter(e->e.getApprovalStatus()==ExpenseApprovalStatus.APPROVED&&e.getPaymentStatus()!=ExpensePaymentStatus.PAID).toList();
  long paidCount=all.stream().filter(e->e.getPaymentStatus()==ExpensePaymentStatus.PAID).count();
  return new OverviewResponse(sum(all,Expense::getAmount),sum(pending,Expense::getAmount),pending.size(),sum(unpaid,Expense::getRemainingAmount),unpaid.size(),sum(all,Expense::getPaidAmount),paidCount,d.label);}
 private Specification<Expense> spec(Filter f,Range d){return(r,q,b)->{List<Predicate>p=new ArrayList<>();p.add(b.between(r.get("expenseDate"),d.from,d.to));if(f!=null){
  if(f.categoryId()!=null)p.add(b.equal(r.get("category").get("expenseCategoryId"),f.categoryId()));if(f.expenseHeadId()!=null)p.add(b.equal(r.get("expenseHead").get("expenseHeadId"),f.expenseHeadId()));
  if(f.approvalStatus()!=null)p.add(b.equal(r.get("approvalStatus"),f.approvalStatus()));if(f.paymentStatus()!=null)p.add(b.equal(r.get("paymentStatus"),f.paymentStatus()));
  if(f.q()!=null&&!f.q().isBlank()){String x="%"+f.q().toLowerCase()+"%";p.add(b.or(b.like(b.lower(r.get("expenseNumber")),x),b.like(b.lower(r.get("headNameSnapshot")),x),b.like(b.lower(r.get("vendorName")),x),b.like(b.lower(r.get("vendorInvoiceNumber")),x),b.like(b.lower(r.get("remarks")),x)));}}return b.and(p.toArray(Predicate[]::new));};}
 private Range range(Filter f){LocalDate now=LocalDate.now(),from=f==null?null:f.dateFrom(),to=f==null?null:f.dateTo();String preset=f==null?null:f.datePreset();
  if("THIS_MONTH".equals(preset)){from=now.withDayOfMonth(1);to=from.plusMonths(1).minusDays(1);}else if("THIS_QUARTER".equals(preset)){from=LocalDate.of(now.getYear(),((now.getMonthValue()-1)/3)*3+1,1);to=from.plusMonths(3).minusDays(1);}
  else if("THIS_FY".equals(preset)){Optional<AcademicYear> y=academicYears.findByCurrentYearTrue();from=y.map(AcademicYear::getStartDate).orElse(LocalDate.of(now.getYear(),1,1));to=y.map(AcademicYear::getEndDate).orElse(LocalDate.of(now.getYear(),12,31));}
  if(from==null)from=LocalDate.of(1900,1,1);if(to==null)to=LocalDate.of(2999,12,31);return new Range(from,to,from+" to "+to);}
 private ListRow row(Expense e){return new ListRow(e.getExpenseId(),e.getExpenseDate(),e.getExpenseNumber(),e.getHeadNameSnapshot(),e.getCategoryNameSnapshot(),e.getVendorName(),e.getAmount(),e.getApprovalStatus(),e.getPaymentStatus(),e.getPaidAmount(),e.getRemainingAmount());}
 private BigDecimal sum(List<Expense>x,Function<Expense,BigDecimal>f){return x.stream().map(f).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);}
 private record Range(LocalDate from,LocalDate to,String label){}
}
