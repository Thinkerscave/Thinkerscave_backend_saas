package com.thinkerscave.finance.service.impl;

import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.entity.*;
import com.thinkerscave.finance.enums.BillingPeriodStatus;
import com.thinkerscave.finance.enums.FeeItemType;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import com.thinkerscave.finance.enums.FeeServiceKey;
import com.thinkerscave.finance.repository.*;
import com.thinkerscave.finance.security.FinanceAccessGuard;
import com.thinkerscave.finance.service.FeeGenerationService;
import com.thinkerscave.finance.service.PaymentAllocationService;
import com.thinkerscave.finance.util.FeeDueDateCalculator;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.enums.EnrollmentStatus;
import com.thinkerscave.student.enums.StudentStatus;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeeGenerationServiceImpl implements FeeGenerationService {

    private final FinanceAccessGuard accessGuard;
    private final FeeStructureRepository structureRepository;
    private final FeeStructureItemRepository itemRepository;
    private final FeeHeadRepository feeHeadRepository;
    private final FinanceConfigurationRepository configurationRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final StudentFeeAccountRepository feeAccountRepository;
    private final StudentBillingPeriodRepository billingPeriodRepository;
    private final StudentBillingPeriodLineRepository billingPeriodLineRepository;
    private final FeePaymentRepository paymentRepository;
    private final FeePaymentAllocationRepository allocationRepository;
    private final PaymentAllocationService paymentAllocationService;
    private final AuditWriteService auditWriteService;

    @Override
    @Transactional
    public int generate(Long academicYearId, Long classId) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_SETTINGS);
        return generateSystem(academicYearId, classId);
    }

    @Override
    @Transactional
    public int generateSystem(Long academicYearId, Long classId) {
        AcademicYear year = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        FinanceConfiguration config = configurationRepository.findAll().stream().findFirst().orElse(null);
        int leadDays = config != null && config.getGenerationLeadDays() != null ? config.getGenerationLeadDays() : 5;
        LocalDate today = LocalDate.now();

        List<FeeStructure> structures = structureRepository.findByStatus(FeeMasterStatus.ACTIVE).stream()
                .filter(s -> s.getAcademicYearId().equals(academicYearId))
                .filter(s -> classId == null || classId.equals(s.getClassId()))
                .toList();

        int created = 0;
        for (FeeStructure structure : structures) {
            List<FeeStructureItem> items = itemRepository.findByFeeStructureIdOrderByFeeStructureItemIdAsc(structure.getFeeStructureId());
            List<StudentEnrollment> enrollments = enrollmentRepository.findActiveByAcademicYearId(academicYearId).stream()
                    .filter(e -> e.getClassEntity() != null && structure.getClassId().equals(e.getClassEntity().getClassId()))
                    .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                    .toList();

            for (StudentEnrollment enrollment : enrollments) {
                Student student = enrollment.getStudent();
                if (student == null || student.getStatus() != StudentStatus.ACTIVE) continue;
                ensureAndLockAccount(student.getStudentId(), academicYearId);

                List<FeeStructureItem> applicable = items.stream().filter(item -> {
                    if (item.getType() == FeeItemType.MANDATORY) return true;
                    if (item.getServiceKey() == FeeServiceKey.TRANSPORT) {
                        return Boolean.TRUE.equals(student.getTransportRequired());
                    }
                    if (item.getServiceKey() == FeeServiceKey.HOSTEL) {
                        return Boolean.TRUE.equals(student.getHostelRequired());
                    }
                    return false;
                }).toList();

                // Aggregate applicable items by billing period key (structure.dueDay authoritative)
                record Keyed(FeeDueDateCalculator.PeriodSpec spec, List<FeeStructureItem> items) {}
                java.util.LinkedHashMap<String, Keyed> byKey = new java.util.LinkedHashMap<>();
                for (FeeStructureItem item : applicable) {
                    for (FeeDueDateCalculator.PeriodSpec spec : FeeDueDateCalculator.periodsForYear(
                            item.getFrequency(), year.getStartDate(), year.getEndDate(), structure.getDueDay())) {
                        if (today.isBefore(spec.periodStart().minusDays(leadDays))) continue;
                        byKey.compute(spec.periodKey(), (k, existing) -> {
                            if (existing == null) {
                                List<FeeStructureItem> list = new ArrayList<>();
                                list.add(item);
                                return new Keyed(spec, list);
                            }
                            existing.items().add(item);
                            return existing;
                        });
                    }
                }

                for (Keyed keyed : byKey.values()) {
                    if (billingPeriodRepository.findByStudentIdAndAcademicYearIdAndPeriodKey(
                            student.getStudentId(), academicYearId, keyed.spec().periodKey()).isPresent()) {
                        continue;
                    }
                    BigDecimal total = keyed.items().stream().map(FeeStructureItem::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
                    StudentBillingPeriod period = new StudentBillingPeriod();
                    period.setStudentId(student.getStudentId());
                    period.setAcademicYearId(academicYearId);
                    period.setFeeStructureId(structure.getFeeStructureId());
                    period.setPeriodKey(keyed.spec().periodKey());
                    period.setPeriodLabel(keyed.spec().periodLabel());
                    period.setPeriodStart(keyed.spec().periodStart());
                    period.setPeriodEnd(keyed.spec().periodEnd());
                    period.setDueDate(keyed.spec().dueDate());
                    period.setTotalAmount(total);
                    period.setPaidAmount(BigDecimal.ZERO.setScale(2));
                    period.setBalanceAmount(total);
                    period.setStatus(BillingPeriodStatus.DUE);
                    period.setClassId(structure.getClassId());
                    period.setClassName(enrollment.getClassEntity() != null ? enrollment.getClassEntity().getName() : null);
                    period.setSectionId(enrollment.getSection() != null ? enrollment.getSection().getSectionId() : null);
                    period.setSectionName(enrollment.getSection() != null ? enrollment.getSection().getName() : null);
                    period.setStructureName(structure.getName());
                    period = billingPeriodRepository.save(period);

                    for (FeeStructureItem item : keyed.items()) {
                        FeeHead head = feeHeadRepository.findById(item.getFeeHeadId()).orElse(null);
                        StudentBillingPeriodLine line = new StudentBillingPeriodLine();
                        line.setStudentBillingPeriodId(period.getStudentBillingPeriodId());
                        line.setFeeHeadId(item.getFeeHeadId());
                        line.setFeeHeadName(head != null ? head.getName() : "Fee");
                        line.setFeeHeadCategory(head != null && head.getCategory() != null ? head.getCategory().name() : null);
                        line.setAmount(item.getAmount());
                        line.setFrequency(item.getFrequency());
                        line.setType(item.getType());
                        line.setServiceKey(item.getServiceKey());
                        billingPeriodLineRepository.save(line);
                    }
                    created++;
                }

                consumeAdvance(student.getStudentId(), academicYearId);
            }
        }
        auditWriteService.record(AuditEventType.SYSTEM_EVENT, "FEE_GENERATION_RUN", "FeeGeneration",
                String.valueOf(academicYearId), "Generated " + created + " billing periods");
        return created;
    }

    private void consumeAdvance(Long studentId, Long yearId) {
        List<FeePayment> payments = paymentRepository.findByStudentIdAndAcademicYearIdOrderByPaidOnDesc(studentId, yearId)
                .stream()
                .sorted(Comparator.comparing(FeePayment::getPaidOn).thenComparing(FeePayment::getFeePaymentId))
                .toList();
        List<StudentBillingPeriod> open = billingPeriodRepository.findOpenPeriodsForUpdate(studentId, yearId);
        LocalDate today = LocalDate.now();
        for (FeePayment payment : payments) {
            BigDecimal allocated = scale(allocationRepository.sumForPayment(payment.getFeePaymentId()));
            BigDecimal remaining = scale(payment.getAmount()).subtract(allocated);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) continue;
            var plan = paymentAllocationService.plan(remaining, open);
            for (var a : plan.allocations()) {
                StudentBillingPeriod period = open.stream()
                        .filter(p -> p.getStudentBillingPeriodId().equals(a.periodId())).findFirst().orElse(null);
                if (period == null) continue;
                period.setPaidAmount(scale(period.getPaidAmount()).add(a.amount()));
                period.setBalanceAmount(scale(period.getTotalAmount()).subtract(period.getPaidAmount()));
                paymentAllocationService.applyStatus(period, today);
                billingPeriodRepository.save(period);
                FeePaymentAllocation alloc = new FeePaymentAllocation();
                alloc.setFeePaymentId(payment.getFeePaymentId());
                alloc.setStudentBillingPeriodId(period.getStudentBillingPeriodId());
                alloc.setAllocatedAmount(a.amount());
                allocationRepository.save(alloc);
            }
            open = billingPeriodRepository.findOpenPeriodsForUpdate(studentId, yearId);
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

    private static BigDecimal scale(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }
}
