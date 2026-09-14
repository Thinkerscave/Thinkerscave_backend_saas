package com.thinkerscave.finance.expense.service.impl;
import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.expense.dto.ExpenseDtos.*;
import com.thinkerscave.finance.expense.entity.ExpenseConfiguration;
import com.thinkerscave.finance.expense.repository.ExpenseRepository;
import com.thinkerscave.finance.expense.security.ExpenseAccessGuard;
import com.thinkerscave.finance.expense.service.*;
import com.thinkerscave.shared.document.dto.StoreDocumentCommand;
import com.thinkerscave.shared.document.entity.ManagedDocument;
import com.thinkerscave.shared.document.enums.*;
import com.thinkerscave.shared.document.repository.ManagedDocumentRepository;
import com.thinkerscave.shared.document.service.DocumentManagementService;
import com.thinkerscave.shared.exceptions.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ExpenseAttachmentServiceImpl implements ExpenseAttachmentService {
 private static final String OWNER="EXPENSE";private final ExpenseRepository expenses;private final ExpenseSettingsService settings;
 private final DocumentManagementService documents;private final ManagedDocumentRepository repository;private final ExpenseAccessGuard guard;
 private final ExpenseDtoMapper mapper;private final AuditWriteService audit;
 @Transactional public AttachmentResponse upload(Long id,MultipartFile file,String kind){guard.requireManage(ExpenseAccessGuard.EXPENSES);requireExpense(id);ExpenseConfiguration c=settings.requireConfig();if(file==null||file.isEmpty())throw new BadRequestException("Attachment file is required");if(file.getSize()>c.getMaxAttachmentBytes())throw new BadRequestException("Attachment exceeds configured size");
  String type=Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");if(Arrays.stream(c.getAllowedAttachmentContentTypes().split(",")).map(String::trim).noneMatch(type::equalsIgnoreCase))throw new BadRequestException("Attachment content type is not allowed");
  try{String k=kind==null||kind.isBlank()?"VENDOR_INVOICE":kind;ManagedDocument d=documents.store(StoreDocumentCommand.builder().documentType(DocumentType.EXPENSE_ATTACHMENT).ownerType(OWNER).ownerId(id).periodKey(k).idempotencyKey(k+":"+id+":"+UUID.randomUUID()).content(file.getBytes()).contentType(type).fileName(file.getOriginalFilename()).build());audit.record(AuditEventType.CREATE,"EXPENSE_ATTACHMENT_ADD","ManagedDocument",String.valueOf(d.getManagedDocumentId()),"Expense attachment added");return mapper.attachment(d);}catch(java.io.IOException ex){throw new BadRequestException("Failed to read attachment");}}
 public List<AttachmentResponse> list(Long id){guard.requireView(ExpenseAccessGuard.EXPENSES);requireExpense(id);return docs(id).stream().map(mapper::attachment).toList();}
 public Download download(Long id,Long docId){guard.requireView(ExpenseAccessGuard.EXPENSES);ManagedDocument d=requireDocument(id,docId);return new Download(documents.loadContent(docId),d.getFileName(),d.getContentType());}
 @Transactional public void delete(Long id,Long docId){guard.requireManage(ExpenseAccessGuard.EXPENSES);ManagedDocument d=requireDocument(id,docId);d.setStatus(ManagedDocumentStatus.SUPERSEDED);repository.save(d);audit.record(AuditEventType.UPDATE,"EXPENSE_ATTACHMENT_REMOVE","ManagedDocument",String.valueOf(docId),"Expense attachment removed");}
 private void requireExpense(Long id){if(!expenses.existsById(id))throw new ResourceNotFoundException("Expense not found: "+id);}
 private List<ManagedDocument> docs(Long id){return repository.findByDocumentTypeAndOwnerTypeAndOwnerIdAndStatusOrderByCreatedOnAsc(DocumentType.EXPENSE_ATTACHMENT,OWNER,id,ManagedDocumentStatus.ACTIVE);}
 private ManagedDocument requireDocument(Long id,Long docId){ManagedDocument d=documents.findById(docId);if(d.getDocumentType()!=DocumentType.EXPENSE_ATTACHMENT||!OWNER.equals(d.getOwnerType())||!id.equals(d.getOwnerId())||d.getStatus()!=ManagedDocumentStatus.ACTIVE)throw new ResourceNotFoundException("Expense attachment not found");return d;}
}
