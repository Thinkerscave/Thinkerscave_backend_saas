package com.thinkerscave.finance.security;

import com.thinkerscave.staff.entity.ResponsibilityAssignment;
import com.thinkerscave.staff.entity.ResponsibilityAssignmentScope;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.DataScopeType;
import com.thinkerscave.staff.repository.ResponsibilityAssignmentRepository;
import com.thinkerscave.staff.repository.ResponsibilityAssignmentScopeRepository;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.entity.StudentParent;
import com.thinkerscave.student.enums.StudentStatus;
import com.thinkerscave.student.repository.ParentRepository;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentParentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves which students the current user may see in Finance.
 * Uses Student/Parent linkage and structured ResponsibilityAssignmentScope only —
 * never parses free-text ResponsibilityAssignment.scope.
 * Organization Admin/Owner with fee student permissions get organization-wide search
 * (still gated by PermissionService via {@link FinanceAccessGuard}).
 */
@Component
@RequiredArgsConstructor
public class FinanceDataScopeResolver {

    private final FinanceAccessGuard accessGuard;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final StudentParentRepository studentParentRepository;
    private final StaffRepository staffRepository;
    private final ResponsibilityAssignmentRepository assignmentRepository;
    private final ResponsibilityAssignmentScopeRepository scopeRepository;
    private final StudentEnrollmentRepository enrollmentRepository;

    public record ScopeResult(
            Set<Long> accessibleStudentIds,
            boolean canSearchStudents,
            boolean organizationWide
    ) {}

    public ScopeResult resolve() {
        Long userId = accessGuard.currentUserIdOrNull();
        if (userId == null) {
            return new ScopeResult(Set.of(), false, false);
        }

        Set<Long> ids = new HashSet<>();
        boolean canSearch = false;
        boolean orgWide = false;

        studentRepository.findByUser_Id(userId).ifPresent(s -> ids.add(s.getStudentId()));

        parentRepository.findByUser_Id(userId).ifPresent(parent -> {
            List<StudentParent> links = studentParentRepository.findByParent_ParentIdAndActiveTrue(parent.getParentId());
            for (StudentParent link : links) {
                if (link.getStudent() != null) {
                    ids.add(link.getStudent().getStudentId());
                }
            }
        });

        // Org Admin/Owner with student-fee permission: organization-wide (permission still required).
        if (isOrganizationAdminOrOwner() && hasFinanceStudentSearchPermission()) {
            orgWide = true;
            canSearch = true;
            ids.addAll(activeStudentIds());
            return new ScopeResult(Set.copyOf(ids), canSearch, orgWide);
        }

        Staff staff = staffRepository.findByUser_Id(userId).orElse(null);
        if (staff != null) {
            LocalDate today = LocalDate.now();
            List<ResponsibilityAssignment> assignments =
                    assignmentRepository.findByStaff_StaffIdAndActiveTrueOrderByEffectiveFromDesc(staff.getStaffId())
                            .stream()
                            .filter(a -> inDate(a, today))
                            .toList();
            if (!assignments.isEmpty()) {
                List<Long> assignmentIds = assignments.stream()
                        .map(ResponsibilityAssignment::getAssignmentId)
                        .toList();
                List<ResponsibilityAssignmentScope> scopes = scopeRepository.findByAssignmentIdIn(assignmentIds);
                for (ResponsibilityAssignmentScope scope : scopes) {
                    DataScopeType type = scope.getScopeType();
                    if (type == DataScopeType.ORGANIZATION) {
                        orgWide = true;
                        canSearch = true;
                        ids.addAll(activeStudentIds());
                    } else if (type == DataScopeType.CLASS && scope.getClassId() != null) {
                        canSearch = true;
                        enrollmentRepository.findByClassEntityClassIdAndActiveTrueOrderByStudentFirstNameAsc(scope.getClassId())
                                .forEach(e -> ids.add(e.getStudent().getStudentId()));
                    } else if (type == DataScopeType.SECTION && scope.getSectionId() != null) {
                        canSearch = true;
                        Long classId = scope.getClassId();
                        if (classId != null) {
                            enrollmentRepository
                                    .findByClassEntityClassIdAndSectionSectionIdAndActiveTrueOrderByRollNumber(
                                            classId, scope.getSectionId())
                                    .forEach(e -> ids.add(e.getStudent().getStudentId()));
                        } else {
                            addStudentsBySection(scope.getSectionId(), ids);
                        }
                    } else if (type == DataScopeType.STUDENT && scope.getStudentId() != null) {
                        ids.add(scope.getStudentId());
                    }
                }
            }
        }

        return new ScopeResult(Set.copyOf(ids), canSearch, orgWide);
    }

    public void assertStudentAccess(Long studentId) {
        ScopeResult scope = resolve();
        if (studentId == null || !scope.accessibleStudentIds().contains(studentId)) {
            throw new AccessDeniedException("Student not in finance data scope");
        }
    }

    private boolean hasFinanceStudentSearchPermission() {
        return accessGuard.canView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS)
                || accessGuard.canView(FinanceAccessGuard.RESOURCE_MANAGEMENT)
                || accessGuard.canView(FinanceAccessGuard.RESOURCE_OUTSTANDING)
                || accessGuard.canManage(FinanceAccessGuard.RESOURCE_COLLECTION);
    }

    private boolean isOrganizationAdminOrOwner() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String a = authority.getAuthority();
            if ("ORGANIZATION_ADMIN".equals(a) || "ORGANIZATION_OWNER".equals(a) || "SUPER_ADMIN".equals(a)) {
                return true;
            }
        }
        return false;
    }

    private Set<Long> activeStudentIds() {
        return studentRepository.findByStatus(StudentStatus.ACTIVE, org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .map(s -> s.getStudentId())
                .collect(Collectors.toSet());
    }

    private static boolean inDate(ResponsibilityAssignment a, LocalDate today) {
        if (a.getEffectiveFrom() != null && today.isBefore(a.getEffectiveFrom())) {
            return false;
        }
        if (a.getEffectiveTo() != null && today.isAfter(a.getEffectiveTo())) {
            return false;
        }
        return Boolean.TRUE.equals(a.getActive());
    }

    private void addStudentsBySection(Long sectionId, Set<Long> ids) {
        enrollmentRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getActive())
                        && e.getSection() != null
                        && sectionId.equals(e.getSection().getSectionId())
                        && e.getStudent() != null)
                .map(StudentEnrollment::getStudent)
                .map(s -> s.getStudentId())
                .forEach(ids::add);
    }
}
