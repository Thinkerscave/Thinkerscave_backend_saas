package com.thinkerscave.finance.expense.numbering;

import com.thinkerscave.finance.expense.entity.*;
import com.thinkerscave.finance.expense.repository.ExpenseNumberSequenceRepository;
import com.thinkerscave.finance.expense.service.ExpenseSettingsService;
import com.thinkerscave.shared.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service @RequiredArgsConstructor
public class ExpenseNumberServiceImpl implements ExpenseNumberService {
 private final ExpenseNumberSequenceRepository repository; private final ExpenseSettingsService settings;
 @Override @Transactional public String nextNumber(LocalDate date){
  if(date==null)throw new BadRequestException("Expense date is required");
  ExpenseConfiguration c=settings.requireConfig();String key=key(c,date);
  ExpenseNumberSequence s=repository.findBySequenceKeyForUpdate(key).orElseGet(()->{ExpenseNumberSequence n=new ExpenseNumberSequence();n.setSequenceKey(key);n.setLastValue(0L);return repository.saveAndFlush(n);});
  long next=Math.max((s.getLastValue()==null?0:s.getLastValue())+1,c.getNumberStart());s.setLastValue(next);repository.save(s);return format(c,date,next);
 }
 @Override @Transactional(readOnly=true) public String preview(LocalDate date){LocalDate d=date==null?LocalDate.now():date;ExpenseConfiguration c=settings.requireConfig();
  long next=Math.max(repository.findBySequenceKey(key(c,d)).map(ExpenseNumberSequence::getLastValue).orElse(0L)+1,c.getNumberStart());return format(c,d,next);}
 private String key(ExpenseConfiguration c,LocalDate d){return Boolean.TRUE.equals(c.getNumberSequencePerYear())?"EXPENSE:"+d.getYear():"EXPENSE";}
 private String format(ExpenseConfiguration c,LocalDate d,long n){String y="YY".equalsIgnoreCase(c.getNumberYearFormat())?String.format("%02d",d.getYear()%100):String.valueOf(d.getYear());return c.getNumberPrefix()+"-"+y+"-"+String.format("%0"+c.getNumberPadWidth()+"d",n);}
}
