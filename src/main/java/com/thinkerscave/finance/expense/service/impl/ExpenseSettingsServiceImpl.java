package com.thinkerscave.finance.expense.service.impl;
import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.expense.dto.ExpenseDtos.*;
import com.thinkerscave.finance.expense.entity.ExpenseConfiguration;
import com.thinkerscave.finance.expense.repository.ExpenseConfigurationRepository;
import com.thinkerscave.finance.expense.security.ExpenseAccessGuard;
import com.thinkerscave.finance.expense.service.ExpenseSettingsService;
import com.thinkerscave.finance.repository.FeePaymentMethodRepository;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ExpenseSettingsServiceImpl implements ExpenseSettingsService {
 private final ExpenseConfigurationRepository repository; private final FeePaymentMethodRepository methods;
 private final ExpenseAccessGuard guard; private final AuditWriteService audit;
 @Override public SettingsResponse get(){guard.requireView(ExpenseAccessGuard.EXPENSE_SETTINGS);return map(requireConfig());}
 @Override @Transactional public SettingsResponse update(SettingsRequest r){guard.requireManage(ExpenseAccessGuard.EXPENSE_SETTINGS);ExpenseConfiguration c=requireConfig();
  c.setApprovalRequired(r.approvalRequired());c.setNumberPrefix(r.numberPrefix().trim());c.setNumberYearFormat(r.numberYearFormat());
  c.setNumberPadWidth(r.numberPadWidth());c.setNumberStart(r.numberStart());c.setNumberSequencePerYear(r.numberSequencePerYear());
  if(r.defaultPaymentMethodId()!=null&&!methods.existsById(r.defaultPaymentMethodId()))throw new ResourceNotFoundException("Payment method not found");
  c.setDefaultPaymentMethodId(r.defaultPaymentMethodId());
  c.setMaxAttachmentBytes(r.maxAttachmentBytes());c.setAllowedAttachmentContentTypes(r.allowedAttachmentContentTypes());
  repository.save(c);audit.record(AuditEventType.CONFIG_CHANGE,"EXPENSE_SETTINGS_UPDATE","ExpenseConfiguration",String.valueOf(c.getExpenseConfigurationId()),"Expense settings updated");return map(c);}
 @Override @Transactional public ExpenseConfiguration requireConfig(){return repository.findAll().stream().findFirst().orElseGet(()->repository.save(new ExpenseConfiguration()));}
 private SettingsResponse map(ExpenseConfiguration c){return new SettingsResponse(c.getApprovalRequired(),c.getNumberPrefix(),c.getNumberYearFormat(),c.getNumberPadWidth(),c.getNumberStart(),c.getNumberSequencePerYear(),c.getDefaultPaymentMethodId(),c.getMaxAttachmentBytes(),c.getAllowedAttachmentContentTypes());}
}
