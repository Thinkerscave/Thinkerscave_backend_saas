package com.thinkerscave.finance.service.impl;

import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.finance.dto.response.*;
import com.thinkerscave.finance.entity.*;
import com.thinkerscave.finance.enums.BillingPeriodStatus;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import com.thinkerscave.finance.repository.*;
import com.thinkerscave.finance.security.FinanceAccessGuard;
import com.thinkerscave.finance.security.FinanceDataScopeResolver;
import com.thinkerscave.finance.service.FeeReceiptService;
import com.thinkerscave.finance.service.StudentFeeService;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.entity.StudentParent;
import com.thinkerscave.student.repository.ParentRepository;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentParentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentFeeServiceImpl implements StudentFeeService {

    private final FinanceAccessGuard accessGuard;
    private final FinanceDataScopeResolver scopeResolver;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentBillingPeriodRepository billingPeriodRepository;
    private final StudentBillingPeriodLineRepository billingPeriodLineRepository;
    private final FeePaymentRepository paymentRepository;
    private final FeePaymentAllocationRepository allocationRepository;
    private final FeeReceiptRepository receiptRepository;
    private final FeeReceiptService feeReceiptService;
    private final FeeStructureRepository feeStructureRepository;
    private final FeeStructureItemRepository feeStructureItemRepository;
    private final FeeHeadRepository feeHeadRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ParentRepository parentRepository;
    private final StudentParentRepository studentParentRepository;

    @Override
    public PageResponse<StudentFeeListItemResponse> list(
            String q,
            Long academicYearId,
            Long classId,
            Long sectionId,
            BillingPeriodStatus status,
            String periodKey,
            Boolean outstandingOnly,
            Pageable pageable) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        List<StudentFeeListItemResponse> all = collectFiltered(q, academicYearId, classId, sectionId,
                status, periodKey, outstandingOnly);
        sortList(all, pageable.getSort());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<StudentFeeListItemResponse> pageContent = start >= all.size() ? List.of() : all.subList(start, end);
        return PageResponse.of(new PageImpl<>(pageContent, pageable, all.size()));
    }

    @Override
    public StudentFeeSummaryResponse summary(
            String q,
            Long academicYearId,
            Long classId,
            Long sectionId,
            BillingPeriodStatus status,
            String periodKey,
            Boolean outstandingOnly) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        List<StudentFeeListItemResponse> all = collectFiltered(q, academicYearId, classId, sectionId,
                status, periodKey, outstandingOnly);
        BigDecimal totalFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal paid = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal outstanding = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal overdue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (StudentFeeListItemResponse row : all) {
            totalFee = totalFee.add(nz(row.getTotalFee()));
            paid = paid.add(nz(row.getPaid()));
            outstanding = outstanding.add(nz(row.getOutstanding()));
            overdue = overdue.add(nz(row.getOverdue()));
        }
        return StudentFeeSummaryResponse.builder()
                .totalFee(scale(totalFee))
                .paid(scale(paid))
                .outstanding(scale(outstanding))
                .overdue(scale(overdue))
                .studentCount(all.size())
                .build();
    }

    @Override
    public List<BillingPeriodOptionResponse> billingPeriodOptions(Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        var scope = scopeResolver.resolve();
        if (!scope.canSearchStudents()) {
            throw new AccessDeniedException("Student search requires structured CLASS/SECTION/ORGANIZATION scope");
        }
        Long yearId = academicYearId != null ? academicYearId : currentYearId();
        List<Long> ids = scope.organizationWide() ? null : new ArrayList<>(scope.accessibleStudentIds());
        List<BillingPeriodOptionResponse> out = new ArrayList<>();
        for (Object[] row : billingPeriodRepository.distinctPeriodsForYear(yearId, ids)) {
            out.add(BillingPeriodOptionResponse.builder()
                    .periodKey(String.valueOf(row[0]))
                    .periodLabel(String.valueOf(row[1]))
                    .build());
        }
        return out;
    }

    @Override
    public List<StudentFeeListItemResponse> search(String q, Long academicYearId) {
        boolean allowed = accessGuard.canManage(FinanceAccessGuard.RESOURCE_COLLECTION)
                || (accessGuard.canView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS) && scopeResolver.resolve().canSearchStudents());
        if (!allowed) {
            throw new AccessDeniedException("Student fee search not permitted");
        }
        var scope = scopeResolver.resolve();
        Long yearId = academicYearId != null ? academicYearId : currentYearId();
        List<StudentFeeListItemResponse> out = new ArrayList<>();
        String needle = q == null ? "" : q.trim().toLowerCase();
        for (Long studentId : scope.accessibleStudentIds()) {
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) continue;
            String hay = (student.getFirstName() + " " + student.getLastName() + " " + student.getAdmissionNumber()).toLowerCase();
            if (!needle.isEmpty() && !hay.contains(needle)) continue;
            out.add(toListItem(student, enrollmentRepository.findActiveWithClassByStudentId(studentId).orElse(null), yearId));
            if (out.size() >= 20) break;
        }
        return out;
    }

    @Override
    public StudentFeeDetailResponse detail(Long studentId, Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        scopeResolver.assertStudentAccess(studentId);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        Long yearId = academicYearId != null ? academicYearId : currentYearId();
        AcademicYear year = academicYearRepository.findById(yearId).orElse(null);
        StudentEnrollment enr = enrollmentRepository.findActiveWithClassByStudentId(studentId).orElse(null);
        return StudentFeeDetailResponse.builder()
                .studentId(studentId)
                .studentName((student.getFirstName() + " " + student.getLastName()).trim())
                .admissionNumber(student.getAdmissionNumber())
                .className(enr != null && enr.getClassEntity() != null ? enr.getClassEntity().getName() : null)
                .sectionName(enr != null && enr.getSection() != null ? enr.getSection().getName() : null)
                .academicYearId(yearId)
                .academicYearName(year != null ? year.getName() : null)
                .kpis(computeKpis(studentId, yearId))
                .canCollectFee(accessGuard.canManage(FinanceAccessGuard.RESOURCE_COLLECTION))
                .structureBreakdown(structureBreakdown(enr, yearId))
                .build();
    }

    @Override
    public List<BillingPeriodResponse> periods(Long studentId, Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        scopeResolver.assertStudentAccess(studentId);
        Long yearId = academicYearId != null ? academicYearId : currentYearId();
        return billingPeriodRepository
                .findByStudentIdAndAcademicYearIdOrderByDueDateAscPeriodStartAscStudentBillingPeriodIdAsc(studentId, yearId)
                .stream().map(this::toPeriod).toList();
    }

    @Override
    public List<FeePaymentResponse> payments(Long studentId, Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        scopeResolver.assertStudentAccess(studentId);
        Long yearId = academicYearId != null ? academicYearId : currentYearId();
        return paymentRepository.findByStudentIdAndAcademicYearIdOrderByPaidOnDesc(studentId, yearId).stream()
                .map(p -> {
                    var receipt = receiptRepository.findByFeePaymentId(p.getFeePaymentId()).orElse(null);
                    Student student = studentRepository.findById(p.getStudentId()).orElse(null);
                    String studentName = student == null ? null
                            : ((student.getFirstName() == null ? "" : student.getFirstName()) + " "
                            + (student.getLastName() == null ? "" : student.getLastName())).trim();
                    return FeePaymentResponse.builder()
                            .feePaymentId(p.getFeePaymentId())
                            .studentId(p.getStudentId())
                            .studentName(studentName)
                            .admissionNumber(student != null ? student.getAdmissionNumber() : null)
                            .academicYearId(p.getAcademicYearId())
                            .paymentMethodId(p.getPaymentMethodId())
                            .paymentMethodName(p.getPaymentMethodName())
                            .amount(p.getAmount())
                            .paidOn(p.getPaidOn())
                            .referenceNumber(p.getReferenceNumber())
                            .remarks(p.getRemarks())
                            .status(p.getStatus())
                            .receiptId(receipt != null ? receipt.getFeeReceiptId() : null)
                            .receiptNumber(receipt != null ? receipt.getReceiptNumber() : null)
                            .build();
                })
                .toList();
    }

    @Override
    public List<FeeReceiptResponse> receipts(Long studentId, Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        scopeResolver.assertStudentAccess(studentId);
        Long yearId = academicYearId != null ? academicYearId : currentYearId();
        return receiptRepository.findByStudentIdAndAcademicYearIdOrderByIssuedOnDesc(studentId, yearId).stream()
                .map(r -> feeReceiptService.get(r.getFeeReceiptId()))
                .toList();
    }

    @Override
    public List<AcademicYearOptionResponse> academicYears(Long studentId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        scopeResolver.assertStudentAccess(studentId);
        return academicYearRepository.findAll().stream()
                .map(y -> AcademicYearOptionResponse.builder()
                        .academicYearId(y.getAcademicYearId())
                        .name(y.getName())
                        .build())
                .toList();
    }

    @Override
    public StudentFeeKpiResponse outstanding(Long studentId, Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        scopeResolver.assertStudentAccess(studentId);
        return computeKpis(studentId, academicYearId != null ? academicYearId : currentYearId());
    }

    @Override
    public List<LinkedStudentResponse> linked() {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        Long userId = accessGuard.currentUserIdOrNull();
        List<LinkedStudentResponse> out = new ArrayList<>();
        if (userId == null) return out;
        studentRepository.findByUser_Id(userId).ifPresent(s -> out.add(LinkedStudentResponse.builder()
                .studentId(s.getStudentId())
                .studentName((s.getFirstName() + " " + s.getLastName()).trim())
                .admissionNumber(s.getAdmissionNumber())
                .relationship("SELF")
                .build()));
        parentRepository.findByUser_Id(userId).ifPresent(parent -> {
            for (StudentParent link : studentParentRepository.findByParent_ParentIdAndActiveTrue(parent.getParentId())) {
                Student s = link.getStudent();
                if (s == null) continue;
                out.add(LinkedStudentResponse.builder()
                        .studentId(s.getStudentId())
                        .studentName((s.getFirstName() + " " + s.getLastName()).trim())
                        .admissionNumber(s.getAdmissionNumber())
                        .relationship(link.getRelationship() != null ? link.getRelationship().name() : "PARENT")
                        .build());
            }
        });
        return out;
    }

    private List<StudentFeeListItemResponse> collectFiltered(
            String q,
            Long academicYearId,
            Long classId,
            Long sectionId,
            BillingPeriodStatus status,
            String periodKey,
            Boolean outstandingOnly) {
        var scope = scopeResolver.resolve();
        if (!scope.canSearchStudents()) {
            throw new AccessDeniedException("Student search requires structured CLASS/SECTION/ORGANIZATION scope");
        }
        Long yearId = academicYearId != null ? academicYearId : currentYearId();
        List<StudentFeeListItemResponse> all = new ArrayList<>();
        for (Long studentId : scope.accessibleStudentIds()) {
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) continue;
            if (q != null && !q.isBlank()) {
                String needle = q.trim().toLowerCase();
                String hay = (student.getFirstName() + " " + student.getLastName() + " " + student.getAdmissionNumber()).toLowerCase();
                if (!hay.contains(needle)) continue;
            }
            StudentEnrollment enr = enrollmentRepository.findActiveWithClassByStudentId(studentId).orElse(null);
            if (classId != null && (enr == null || enr.getClassEntity() == null || !classId.equals(enr.getClassEntity().getClassId()))) continue;
            if (sectionId != null && (enr == null || enr.getSection() == null || !sectionId.equals(enr.getSection().getSectionId()))) continue;
            if (periodKey != null && !periodKey.isBlank()
                    && !billingPeriodRepository.existsByStudentIdAndAcademicYearIdAndPeriodKey(studentId, yearId, periodKey.trim())) {
                continue;
            }
            StudentFeeListItemResponse item = toListItem(student, enr, yearId);
            if (Boolean.TRUE.equals(outstandingOnly) && nz(item.getOutstanding()).compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (status != null && item.getStatus() != status) {
                continue;
            }
            all.add(item);
        }
        return all;
    }

    private void sortList(List<StudentFeeListItemResponse> all, Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            all.sort(Comparator
                    .comparing((StudentFeeListItemResponse r) -> nz(r.getOutstanding())).reversed()
                    .thenComparing(r -> r.getStudentName() == null ? "" : r.getStudentName(), String.CASE_INSENSITIVE_ORDER));
            return;
        }
        Sort.Order order = sort.iterator().next();
        String prop = order.getProperty();
        Comparator<StudentFeeListItemResponse> cmp = switch (prop) {
            case "outstanding" -> Comparator.comparing(r -> nz(r.getOutstanding()));
            case "paid" -> Comparator.comparing(r -> nz(r.getPaid()));
            case "totalFee" -> Comparator.comparing(r -> nz(r.getTotalFee()));
            case "overdue" -> Comparator.comparing(r -> nz(r.getOverdue()));
            case "studentName", "fullName" -> Comparator.comparing(
                    r -> r.getStudentName() == null ? "" : r.getStudentName(), String.CASE_INSENSITIVE_ORDER);
            case "admissionNumber" -> Comparator.comparing(
                    r -> r.getAdmissionNumber() == null ? "" : r.getAdmissionNumber(), String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(r -> nz(r.getOutstanding()));
        };
        if (order.isDescending()) {
            cmp = cmp.reversed();
        }
        all.sort(cmp.thenComparing(r -> r.getStudentName() == null ? "" : r.getStudentName(), String.CASE_INSENSITIVE_ORDER));
    }

    private StudentFeeListItemResponse toListItem(Student student, StudentEnrollment enr, Long yearId) {
        StudentFeeKpiResponse kpis = computeKpis(student.getStudentId(), yearId);
        return StudentFeeListItemResponse.builder()
                .studentId(student.getStudentId())
                .studentName((student.getFirstName() + " " + student.getLastName()).trim())
                .admissionNumber(student.getAdmissionNumber())
                .className(enr != null && enr.getClassEntity() != null ? enr.getClassEntity().getName() : null)
                .sectionName(enr != null && enr.getSection() != null ? enr.getSection().getName() : null)
                .totalFee(kpis.getTotalFee())
                .paid(kpis.getPaid())
                .outstanding(kpis.getOutstanding())
                .overdue(kpis.getOverdue())
                .advance(kpis.getAdvance())
                .status(deriveStatus(kpis))
                .canCollectFee(accessGuard.canManage(FinanceAccessGuard.RESOURCE_COLLECTION))
                .build();
    }

    private BillingPeriodStatus deriveStatus(StudentFeeKpiResponse kpis) {
        if (nz(kpis.getOverdue()).compareTo(BigDecimal.ZERO) > 0) {
            return BillingPeriodStatus.OVERDUE;
        }
        if (nz(kpis.getOutstanding()).compareTo(BigDecimal.ZERO) <= 0) {
            return BillingPeriodStatus.PAID;
        }
        if (nz(kpis.getPaid()).compareTo(BigDecimal.ZERO) > 0) {
            return BillingPeriodStatus.PARTIALLY_PAID;
        }
        return BillingPeriodStatus.DUE;
    }

    private StudentFeeKpiResponse computeKpis(Long studentId, Long yearId) {
        BigDecimal total = scale(billingPeriodRepository.sumTotal(studentId, yearId));
        BigDecimal paid = scale(billingPeriodRepository.sumPaid(studentId, yearId));
        BigDecimal outstanding = scale(billingPeriodRepository.sumBalance(studentId, yearId));
        BigDecimal overdue = scale(billingPeriodRepository.sumOverdue(studentId, yearId, BillingPeriodStatus.OVERDUE));
        BigDecimal payments = scale(paymentRepository.sumPayments(studentId, yearId));
        BigDecimal allocations = scale(allocationRepository.sumAllocations(studentId, yearId));
        return StudentFeeKpiResponse.builder()
                .totalFee(total)
                .paid(paid)
                .outstanding(outstanding)
                .overdue(overdue)
                .advance(payments.subtract(allocations).max(BigDecimal.ZERO.setScale(2)))
                .build();
    }

    private List<FeeStructureBreakdownItemResponse> structureBreakdown(StudentEnrollment enr, Long yearId) {
        if (enr == null || enr.getClassEntity() == null || yearId == null) {
            return List.of();
        }
        return feeStructureRepository
                .findByAcademicYearIdAndClassIdAndStatus(yearId, enr.getClassEntity().getClassId(), FeeMasterStatus.ACTIVE)
                .map(structure -> feeStructureItemRepository.findByFeeStructureIdOrderByFeeStructureItemIdAsc(structure.getFeeStructureId())
                        .stream()
                        .map(item -> {
                            String headName = feeHeadRepository.findById(item.getFeeHeadId())
                                    .map(FeeHead::getName)
                                    .orElse("Fee Head #" + item.getFeeHeadId());
                            return FeeStructureBreakdownItemResponse.builder()
                                    .feeHeadName(headName)
                                    .frequency(item.getFrequency())
                                    .amount(item.getAmount())
                                    .build();
                        })
                        .toList())
                .orElse(List.of());
    }

    private BillingPeriodResponse toPeriod(StudentBillingPeriod p) {
        List<BillingPeriodLineResponse> lines = billingPeriodLineRepository
                .findByStudentBillingPeriodIdOrderByStudentBillingPeriodLineIdAsc(p.getStudentBillingPeriodId())
                .stream()
                .map(l -> BillingPeriodLineResponse.builder()
                        .feeHeadId(l.getFeeHeadId())
                        .feeHeadName(l.getFeeHeadName())
                        .feeHeadCategory(l.getFeeHeadCategory())
                        .amount(l.getAmount())
                        .frequency(l.getFrequency())
                        .type(l.getType())
                        .serviceKey(l.getServiceKey())
                        .build())
                .toList();
        return BillingPeriodResponse.builder()
                .studentBillingPeriodId(p.getStudentBillingPeriodId())
                .periodKey(p.getPeriodKey())
                .periodLabel(p.getPeriodLabel())
                .periodStart(p.getPeriodStart())
                .periodEnd(p.getPeriodEnd())
                .dueDate(p.getDueDate())
                .totalAmount(p.getTotalAmount())
                .paidAmount(p.getPaidAmount())
                .balanceAmount(p.getBalanceAmount())
                .status(p.getStatus())
                .lines(lines)
                .build();
    }

    private Long currentYearId() {
        return academicYearRepository.findAll().stream()
                .filter(y -> Boolean.TRUE.equals(y.getActive()))
                .map(AcademicYear::getAcademicYearId)
                .findFirst()
                .orElseGet(() -> academicYearRepository.findAll().stream().findFirst()
                        .map(AcademicYear::getAcademicYearId).orElse(null));
    }

    private static BigDecimal scale(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
