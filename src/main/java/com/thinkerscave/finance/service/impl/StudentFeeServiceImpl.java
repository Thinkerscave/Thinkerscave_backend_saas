package com.thinkerscave.finance.service.impl;

import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.finance.dto.response.*;
import com.thinkerscave.finance.entity.*;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    private final AcademicYearRepository academicYearRepository;
    private final ParentRepository parentRepository;
    private final StudentParentRepository studentParentRepository;

    @Override
    public PageResponse<StudentFeeListItemResponse> list(String q, Long academicYearId, Long classId, Long sectionId, Pageable pageable) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
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
            all.add(toListItem(student, enr, yearId));
        }
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<StudentFeeListItemResponse> pageContent = start >= all.size() ? List.of() : all.subList(start, end);
        return PageResponse.of(new PageImpl<>(pageContent, pageable, all.size()));
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
                .advance(kpis.getAdvance())
                .canCollectFee(accessGuard.canManage(FinanceAccessGuard.RESOURCE_COLLECTION))
                .build();
    }

    private StudentFeeKpiResponse computeKpis(Long studentId, Long yearId) {
        BigDecimal total = scale(billingPeriodRepository.sumTotal(studentId, yearId));
        BigDecimal paid = scale(billingPeriodRepository.sumPaid(studentId, yearId));
        BigDecimal outstanding = scale(billingPeriodRepository.sumBalance(studentId, yearId));
        BigDecimal payments = scale(paymentRepository.sumPayments(studentId, yearId));
        BigDecimal allocations = scale(allocationRepository.sumAllocations(studentId, yearId));
        return StudentFeeKpiResponse.builder()
                .totalFee(total)
                .paid(paid)
                .outstanding(outstanding)
                .advance(payments.subtract(allocations).max(BigDecimal.ZERO.setScale(2)))
                .build();
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
}
