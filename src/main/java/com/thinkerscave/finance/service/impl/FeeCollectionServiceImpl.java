package com.thinkerscave.finance.service.impl;

import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.dto.request.CollectFeeRequest;
import com.thinkerscave.finance.dto.response.AllocationResponse;
import com.thinkerscave.finance.dto.response.CollectFeeResponse;
import com.thinkerscave.finance.dto.response.FeePaymentResponse;
import com.thinkerscave.finance.dto.response.PaymentPreviewResponse;
import com.thinkerscave.finance.entity.*;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import com.thinkerscave.finance.enums.FeePaymentStatus;
import com.thinkerscave.finance.enums.FeeReceiptStatus;
import com.thinkerscave.finance.repository.*;
import com.thinkerscave.finance.security.FinanceAccessGuard;
import com.thinkerscave.finance.security.FinanceDataScopeResolver;
import com.thinkerscave.finance.service.FeeCollectionService;
import com.thinkerscave.finance.service.FeeReceiptNumberService;
import com.thinkerscave.finance.service.PaymentAllocationService;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.entity.OrganizationConfiguration;
import com.thinkerscave.platform.repository.OrganizationConfigurationRepository;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.IdempotencyConflictException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeeCollectionServiceImpl implements FeeCollectionService {

    private final FinanceAccessGuard accessGuard;
    private final FinanceDataScopeResolver scopeResolver;
    private final StudentFeeAccountRepository feeAccountRepository;
    private final StudentBillingPeriodRepository billingPeriodRepository;
    private final FeePaymentRepository paymentRepository;
    private final FeePaymentAllocationRepository allocationRepository;
    private final FeePaymentMethodRepository paymentMethodRepository;
    private final FeeReceiptRepository receiptRepository;
    private final FeeReceiptLineRepository receiptLineRepository;
    private final PaymentAllocationService paymentAllocationService;
    private final FeeReceiptNumberService receiptNumberService;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationConfigurationRepository organizationConfigurationRepository;
    private final AuditWriteService auditWriteService;
    private final ObjectMapper objectMapper;

    @Override
    public PaymentPreviewResponse preview(CollectFeeRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_COLLECTION);
        scopeResolver.assertStudentAccess(request.getStudentId());
        validateRequest(request);
        List<StudentBillingPeriod> open = billingPeriodRepository
                .findByStudentIdAndAcademicYearIdOrderByDueDateAscPeriodStartAscStudentBillingPeriodIdAsc(
                        request.getStudentId(), request.getAcademicYearId()).stream()
                .filter(p -> p.getBalanceAmount() != null && p.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        var plan = paymentAllocationService.plan(request.getAmount(), open);
        return PaymentPreviewResponse.builder()
                .amount(scale(request.getAmount()))
                .allocatedAmount(plan.allocatedTotal())
                .advanceRemaining(plan.remaining())
                .allocations(plan.allocations().stream().map(a -> AllocationResponse.builder()
                        .studentBillingPeriodId(a.periodId())
                        .periodKey(a.periodKey())
                        .periodLabel(a.periodLabel())
                        .allocatedAmount(a.amount())
                        .build()).toList())
                .build();
    }

    @Override
    @Transactional
    public CollectFeeResponse collect(CollectFeeRequest request, String idempotencyKey) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_COLLECTION);
        scopeResolver.assertStudentAccess(request.getStudentId());
        validateRequest(request);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required");
        }
        String hash = hashRequest(request);

        var existing = paymentRepository.findByIdempotencyKey(idempotencyKey.trim());
        if (existing.isPresent()) {
            FeePayment prior = existing.get();
            if (!hash.equals(prior.getRequestHash())) {
                throw new IdempotencyConflictException("Idempotency key reused with different request body");
            }
            return toCollectResponse(prior);
        }

        ensureAndLockAccount(request.getStudentId(), request.getAcademicYearId());
        List<StudentBillingPeriod> open = billingPeriodRepository.findOpenPeriodsForUpdate(
                request.getStudentId(), request.getAcademicYearId());

        FeePaymentMethod method = paymentMethodRepository.findById(request.getPaymentMethodId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found"));
        if (method.getStatus() != FeeMasterStatus.ACTIVE) {
            throw new BadRequestException("Payment method is not ACTIVE");
        }
        if (Boolean.TRUE.equals(method.getRequiresReference())
                && (request.getReferenceNumber() == null || request.getReferenceNumber().isBlank())) {
            throw new BadRequestException("Reference number is required for this payment method");
        }

        FeePayment payment = new FeePayment();
        payment.setStudentId(request.getStudentId());
        payment.setAcademicYearId(request.getAcademicYearId());
        payment.setPaymentMethodId(method.getFeePaymentMethodId());
        payment.setPaymentMethodName(method.getName());
        payment.setAmount(scale(request.getAmount()));
        payment.setPaidOn(request.getPaidOn());
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setRemarks(request.getRemarks());
        payment.setStatus(FeePaymentStatus.SUCCESS);
        payment.setIdempotencyKey(idempotencyKey.trim());
        payment.setRequestHash(hash);
        payment.setCollectedByUserId(accessGuard.currentUserIdOrNull());
        payment = paymentRepository.save(payment);

        var plan = paymentAllocationService.plan(payment.getAmount(), open);
        LocalDate today = LocalDate.now();
        List<AllocationResponse> allocationResponses = new ArrayList<>();
        for (var a : plan.allocations()) {
            StudentBillingPeriod period = open.stream()
                    .filter(p -> p.getStudentBillingPeriodId().equals(a.periodId()))
                    .findFirst()
                    .orElseThrow();
            period.setPaidAmount(scale(period.getPaidAmount()).add(a.amount()));
            period.setBalanceAmount(scale(period.getTotalAmount()).subtract(period.getPaidAmount()));
            paymentAllocationService.applyStatus(period, today);
            billingPeriodRepository.save(period);

            FeePaymentAllocation alloc = new FeePaymentAllocation();
            alloc.setFeePaymentId(payment.getFeePaymentId());
            alloc.setStudentBillingPeriodId(period.getStudentBillingPeriodId());
            alloc.setAllocatedAmount(a.amount());
            allocationRepository.save(alloc);

            allocationResponses.add(AllocationResponse.builder()
                    .feePaymentAllocationId(alloc.getFeePaymentAllocationId())
                    .studentBillingPeriodId(period.getStudentBillingPeriodId())
                    .periodKey(period.getPeriodKey())
                    .periodLabel(period.getPeriodLabel())
                    .allocatedAmount(a.amount())
                    .build());
        }

        FeeReceipt receipt = buildReceipt(payment, request.getStudentId(), request.getAcademicYearId());
        receipt = receiptRepository.save(receipt);
        for (AllocationResponse ar : allocationResponses) {
            FeeReceiptLine line = new FeeReceiptLine();
            line.setFeeReceiptId(receipt.getFeeReceiptId());
            line.setStudentBillingPeriodId(ar.getStudentBillingPeriodId());
            line.setPeriodKey(ar.getPeriodKey());
            line.setPeriodLabel(ar.getPeriodLabel());
            line.setAmount(ar.getAllocatedAmount());
            receiptLineRepository.save(line);
        }

        auditWriteService.record(AuditEventType.CREATE, "FEE_PAYMENT_COLLECT", "FeePayment",
                String.valueOf(payment.getFeePaymentId()), "Collected fee payment");
        auditWriteService.record(AuditEventType.CREATE, "FEE_RECEIPT_GENERATE", "FeeReceipt",
                String.valueOf(receipt.getFeeReceiptId()), "Generated receipt " + receipt.getReceiptNumber());

        return CollectFeeResponse.builder()
                .feePaymentId(payment.getFeePaymentId())
                .feeReceiptId(receipt.getFeeReceiptId())
                .receiptNumber(receipt.getReceiptNumber())
                .amount(payment.getAmount())
                .allocatedAmount(plan.allocatedTotal())
                .advanceRemaining(plan.remaining())
                .allocations(allocationResponses)
                .build();
    }

    @Override
    public FeePaymentResponse getPayment(Long paymentId) {
        FeePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        assertPaymentView(payment);
        return toPaymentResponse(payment);
    }

    @Override
    public List<AllocationResponse> getAllocations(Long paymentId) {
        FeePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        assertPaymentView(payment);
        return allocationRepository.findByFeePaymentId(paymentId).stream().map(a -> {
            StudentBillingPeriod period = billingPeriodRepository.findById(a.getStudentBillingPeriodId()).orElse(null);
            return AllocationResponse.builder()
                    .feePaymentAllocationId(a.getFeePaymentAllocationId())
                    .studentBillingPeriodId(a.getStudentBillingPeriodId())
                    .periodKey(period != null ? period.getPeriodKey() : null)
                    .periodLabel(period != null ? period.getPeriodLabel() : null)
                    .allocatedAmount(a.getAllocatedAmount())
                    .build();
        }).toList();
    }

    private void assertPaymentView(FeePayment payment) {
        boolean ok = accessGuard.canView(FinanceAccessGuard.RESOURCE_COLLECTION)
                || accessGuard.canView(FinanceAccessGuard.RESOURCE_RECEIPTS)
                || accessGuard.canView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        if (!ok) {
            accessGuard.requireView(FinanceAccessGuard.RESOURCE_COLLECTION);
        }
        scopeResolver.assertStudentAccess(payment.getStudentId());
    }

    private void validateRequest(CollectFeeRequest request) {
        if (request.getPaidOn() != null && request.getPaidOn().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("paidOn cannot be in the future");
        }
    }

    private void ensureAndLockAccount(Long studentId, Long yearId) {
        feeAccountRepository.findByStudentIdAndAcademicYearId(studentId, yearId).orElseGet(() -> {
            StudentFeeAccount account = new StudentFeeAccount();
            account.setStudentId(studentId);
            account.setAcademicYearId(yearId);
            return feeAccountRepository.save(account);
        });
        feeAccountRepository.findForUpdate(studentId, yearId)
                .orElseThrow(() -> new ResourceNotFoundException("Student fee account lock failed"));
    }

    private FeeReceipt buildReceipt(FeePayment payment, Long studentId, Long yearId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        AcademicYear year = academicYearRepository.findById(yearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        StudentEnrollment enrollment = enrollmentRepository.findActiveWithClassByStudentId(studentId).orElse(null);
        Long orgId = OrganizationContext.getOrganizationId();
        Organization org = orgId == null ? null : organizationRepository.findById(orgId).orElse(null);
        OrganizationConfiguration cfg = orgId == null ? null
                : organizationConfigurationRepository.findByOrganization_Id(orgId).orElse(null);

        FeeReceipt receipt = new FeeReceipt();
        receipt.setFeePaymentId(payment.getFeePaymentId());
        receipt.setReceiptNumber(receiptNumberService.nextReceiptNumber());
        receipt.setIssuedOn(LocalDateTime.now());
        receipt.setStatus(FeeReceiptStatus.ISSUED);
        receipt.setStudentId(studentId);
        receipt.setStudentName((student.getFirstName() + " " + student.getLastName()).trim());
        receipt.setAdmissionNumber(student.getAdmissionNumber());
        if (enrollment != null) {
            receipt.setClassName(enrollment.getClassEntity() != null ? enrollment.getClassEntity().getName() : null);
            receipt.setSectionName(enrollment.getSection() != null ? enrollment.getSection().getName() : null);
        }
        receipt.setAcademicYearId(yearId);
        receipt.setAcademicYearName(year.getName());
        receipt.setAmount(payment.getAmount());
        receipt.setPaymentMethodName(payment.getPaymentMethodName());
        receipt.setReferenceNumber(payment.getReferenceNumber());
        receipt.setRemarks(payment.getRemarks());
        receipt.setSchoolName(org != null ? org.getOrganizationName() : "School");
        receipt.setCurrencyCode(cfg != null ? cfg.getCurrency() : null);
        return receipt;
    }

    private CollectFeeResponse toCollectResponse(FeePayment payment) {
        FeeReceipt receipt = receiptRepository.findByFeePaymentId(payment.getFeePaymentId()).orElse(null);
        List<AllocationResponse> allocs = getAllocations(payment.getFeePaymentId());
        BigDecimal allocated = allocs.stream().map(AllocationResponse::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return CollectFeeResponse.builder()
                .feePaymentId(payment.getFeePaymentId())
                .feeReceiptId(receipt != null ? receipt.getFeeReceiptId() : null)
                .receiptNumber(receipt != null ? receipt.getReceiptNumber() : null)
                .amount(payment.getAmount())
                .allocatedAmount(allocated)
                .advanceRemaining(scale(payment.getAmount()).subtract(allocated))
                .allocations(allocs)
                .build();
    }

    private FeePaymentResponse toPaymentResponse(FeePayment payment) {
        FeeReceipt receipt = receiptRepository.findByFeePaymentId(payment.getFeePaymentId()).orElse(null);
        var student = studentRepository.findById(payment.getStudentId()).orElse(null);
        String studentName = student == null ? null
                : ((student.getFirstName() == null ? "" : student.getFirstName()) + " "
                + (student.getLastName() == null ? "" : student.getLastName())).trim();
        return FeePaymentResponse.builder()
                .feePaymentId(payment.getFeePaymentId())
                .studentId(payment.getStudentId())
                .studentName(studentName)
                .admissionNumber(student != null ? student.getAdmissionNumber() : null)
                .academicYearId(payment.getAcademicYearId())
                .paymentMethodId(payment.getPaymentMethodId())
                .paymentMethodName(payment.getPaymentMethodName())
                .amount(payment.getAmount())
                .paidOn(payment.getPaidOn())
                .referenceNumber(payment.getReferenceNumber())
                .remarks(payment.getRemarks())
                .status(payment.getStatus())
                .receiptId(receipt != null ? receipt.getFeeReceiptId() : null)
                .receiptNumber(receipt != null ? receipt.getReceiptNumber() : null)
                .allocations(getAllocations(payment.getFeePaymentId()))
                .build();
    }

    private String hashRequest(CollectFeeRequest request) {
        try {
            String canonical = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new BadRequestException("Unable to hash payment request");
        }
    }

    private static BigDecimal scale(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }
}
