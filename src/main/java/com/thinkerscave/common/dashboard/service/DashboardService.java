package com.thinkerscave.common.dashboard.service;

import com.thinkerscave.common.admin.domain.SystemEvent;
import com.thinkerscave.common.admin.repository.SystemEventRepository;
import com.thinkerscave.common.admission.domain.ApplicationAdmission;
import com.thinkerscave.common.admission.domain.ApplicationStatus;
import com.thinkerscave.common.admission.domain.Inquiry;
import com.thinkerscave.common.admission.enums.InquiryStatus;
import com.thinkerscave.common.admission.repository.ApplicationAdmissionRepository;
import com.thinkerscave.common.admission.repository.InquiryRepository;
import com.thinkerscave.common.attendance.domain.Attendance;
import com.thinkerscave.common.attendance.domain.Attendance.AttendanceStatus;
import com.thinkerscave.common.attendance.domain.Attendance.AttendanceType;
import com.thinkerscave.common.attendance.repository.AttendanceRepository;
import com.thinkerscave.common.audit.domain.AuditLog;
import com.thinkerscave.common.audit.repository.AuditLogRepository;
import com.thinkerscave.common.communication.domain.Notification;
import com.thinkerscave.common.communication.repository.NotificationRepository;
import com.thinkerscave.common.config.TenantContext;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.dashboard.domain.DashboardWidgetConfig;
import com.thinkerscave.common.dashboard.dto.DashboardSearchDTO;
import com.thinkerscave.common.dashboard.dto.DashboardWorkspaceDTO;
import com.thinkerscave.common.dashboard.dto.DashboardSummaryDTO;
import com.thinkerscave.common.dashboard.repository.DashboardWidgetConfigRepository;
import com.thinkerscave.common.enrollment.domain.EnrollmentStatus;
import com.thinkerscave.common.enrollment.repository.AcademicEnrollmentRepository;
import com.thinkerscave.common.fee.domain.FeeInvoice;
import com.thinkerscave.common.fee.domain.InvoiceStatus;
import com.thinkerscave.common.fee.repository.FeeInvoiceRepository;
import com.thinkerscave.common.leave.domain.LeaveRequest;
import com.thinkerscave.common.leave.domain.LeaveRequest.LeaveStatus;
import com.thinkerscave.common.leave.repository.LeaveRepository;
import com.thinkerscave.common.menum.domain.Role;
import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.orgm.repository.OrganizationRepository;
import com.thinkerscave.common.orgm.repository.OrganizationUserRepository;
import com.thinkerscave.common.payroll.domain.StaffPayroll;
import com.thinkerscave.common.payroll.repository.PayrollRepository;
import com.thinkerscave.common.security.SecurityUtil;
import com.thinkerscave.common.staff.domain.Staff;
import com.thinkerscave.common.staff.repository.BranchRepository;
import com.thinkerscave.common.staff.repository.DepartmentRepository;
import com.thinkerscave.common.staff.repository.StaffRepository;
import com.thinkerscave.common.student.domain.ClassEntity;
import com.thinkerscave.common.student.domain.Guardian;
import com.thinkerscave.common.student.domain.Student;
import com.thinkerscave.common.student.repository.ClassRepository;
import com.thinkerscave.common.student.repository.GuardianRepository;
import com.thinkerscave.common.student.repository.StudentRepository;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.UserRepository;
import com.thinkerscave.common.workflow.domain.WorkflowConfig;
import com.thinkerscave.common.workflow.repository.WorkflowConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Aggregates the signed-in user's dashboard from live organization data and
 * role-scoped widget configuration.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DashboardService {

    private static final Long DEFAULT_ORG_ID = 1L;
    private static final String DEFAULT_ROLE = "DEFAULT";
    private static final Set<String> KNOWN_ROLES = Set.of(
            "SUPER_ADMIN", "ADMIN", "PRINCIPAL", "TEACHER", "HR_MANAGER", "ACCOUNTANT",
            "RECEPTIONIST", "COUNSELLOR", "PARENT", "STUDENT", "STAFF", "IT_SUPPORT");
    private static final List<String> SEARCH_CATEGORIES = List.of(
            "Students", "Staff", "Parents", "Classes", "Invoices", "Admissions", "Attendance",
            "Departments", "Branches", "Transport", "Hostel");

    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final AcademicEnrollmentRepository enrollmentRepository;
    private final FeeInvoiceRepository feeInvoiceRepository;
    private final InquiryRepository inquiryRepository;
    private final ApplicationAdmissionRepository applicationAdmissionRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRepository leaveRepository;
    private final PayrollRepository payrollRepository;
    private final AuditLogRepository auditLogRepository;
    private final SystemEventRepository systemEventRepository;
    private final NotificationRepository notificationRepository;
    private final WorkflowConfigRepository workflowConfigRepository;
    private final DashboardWidgetConfigRepository widgetConfigRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final ClassRepository classRepository;
    private final GuardianRepository guardianRepository;

    public DashboardSummaryDTO summary(Long academicYearId) {
        Long orgId = currentOrgId();
        long totalStudents = orgId == null ? 0L
                : safe(studentRepository.countByOrganizationId(orgId));
        long activeStudents = orgId == null ? 0L
                : studentRepository.findByOrganizationIdAndIsActive(orgId, true).size();

        List<Staff> staff = orgId == null ? List.of() : staffRepository.findByOrganizationId(orgId);
        long totalStaff = staff.size();
        long activeStaff = orgId == null ? 0L
                : staffRepository.findByOrganizationIdAndIsActive(orgId, Boolean.TRUE).size();

        long activeEnrollments = (orgId != null && academicYearId != null)
                ? enrollmentRepository.countByOrganizationIdAndAcademicYearIdAndStatus(
                        orgId, academicYearId, EnrollmentStatus.ACTIVE)
                : 0L;

        long unpaidInvoices = orgId == null ? 0L
                : feeInvoiceRepository.countByOrganizationIdAndStatus(orgId, InvoiceStatus.ISSUED)
                + feeInvoiceRepository.countByOrganizationIdAndStatus(orgId, InvoiceStatus.PARTIALLY_PAID);
        long overdueInvoices = orgId == null ? 0L
                : feeInvoiceRepository.countByOrganizationIdAndStatus(orgId, InvoiceStatus.OVERDUE);

        return DashboardSummaryDTO.builder()
                .organizationId(orgId)
                .totalStudents(totalStudents)
                .activeStudents(activeStudents)
                .totalStaff(totalStaff)
                .activeStaff(activeStaff)
                .activeEnrollments(activeEnrollments)
                .openInquiries(0L)
                .pendingAdmissions(0L)
                .unpaidInvoices(unpaidInvoices)
                .overdueInvoices(overdueInvoices)
                .build();
    }

        public DashboardWorkspaceDTO workspace() {
                DashboardContext context = currentDashboardContext();
                DashboardSnapshot snapshot = snapshot(context.organizationId());
                List<DashboardWidgetConfig> visibleConfig = visibleWidgetConfig(context.roleCodes());
                List<DashboardWorkspaceDTO.QuickAction> quickActions = quickActions(visibleConfig);

                return new DashboardWorkspaceDTO(
                                userContext(context, snapshot, quickActions),
                                widgets(visibleConfig),
                                kpis(visibleConfig, snapshot),
                                quickActions,
                                priorities(context, snapshot, quickActions),
                                approvals(context, snapshot),
                                activities(snapshot),
                                alerts(snapshot),
                                shortcuts(visibleConfig, snapshot),
                                new DashboardWorkspaceDTO.SearchMeta("Search students, staff, parents, classes, invoices", SEARCH_CATEGORIES));
        }

        public DashboardSearchDTO search(String query) {
                String normalizedQuery = normalize(query);
                if (normalizedQuery.length() < 2) {
                        return new DashboardSearchDTO(query == null ? "" : query, List.of(), SEARCH_CATEGORIES);
                }

                DashboardContext context = currentDashboardContext();
                DashboardSnapshot snapshot = snapshot(context.organizationId());
                List<DashboardSearchDTO.Result> results = new ArrayList<>();

                snapshot.students().stream()
                                .filter(student -> matches(normalizedQuery, student.getFirstName(), student.getLastName(), student.getEmail(), student.getRollNumber()))
                                .limit(8)
                                .forEach(student -> results.add(new DashboardSearchDTO.Result(
                                                "student-" + student.getStudentId(),
                                                "Student",
                                                String.valueOf(student.getStudentId()),
                                                fullName(student.getFirstName(), student.getMiddleName(), student.getLastName()),
                                                student.getRollNumber(),
                                                classLabel(student),
                                                "pi pi-user",
                                                "/app/students/profiles",
                                                tone(Boolean.TRUE.equals(student.getIsActive())),
                                                metadata("email", student.getEmail(), "mobile", student.getMobileNumber()))));

                snapshot.staff().stream()
                                .filter(staffMember -> matches(normalizedQuery, staffMember.getFirstName(), staffMember.getLastName(), staffMember.getEmail(), staffMember.getStaffCode()))
                                .limit(8)
                                .forEach(staffMember -> results.add(new DashboardSearchDTO.Result(
                                                "staff-" + staffMember.getId(),
                                                "Staff",
                                                String.valueOf(staffMember.getId()),
                                                fullName(staffMember.getFirstName(), staffMember.getMiddleName(), staffMember.getLastName()),
                                                staffMember.getStaffCode(),
                                                departmentLabel(staffMember),
                                                "pi pi-briefcase",
                                                "/app/staff/directory",
                                                tone(Boolean.TRUE.equals(staffMember.getIsActive())),
                                                metadata("email", staffMember.getEmail(), "mobile", staffMember.getMobileNumber()))));

                snapshot.guardians().stream()
                                .filter(guardian -> matches(normalizedQuery, guardian.getFirstName(), guardian.getLastName(), guardian.getEmail(), String.valueOf(guardian.getMobileNumber())))
                                .limit(6)
                                .forEach(guardian -> results.add(new DashboardSearchDTO.Result(
                                                "guardian-" + guardian.getGuardianId(),
                                                "Parent",
                                                String.valueOf(guardian.getGuardianId()),
                                                fullName(guardian.getFirstName(), guardian.getMiddleName(), guardian.getLastName()),
                                                guardian.getRelation(),
                                                guardian.getEmail(),
                                                "pi pi-address-book",
                                                "/app/students/parents",
                                                "info",
                                                metadata("mobile", guardian.getMobileNumber()))));

                snapshot.classes().stream()
                                .filter(classEntity -> matches(normalizedQuery, classEntity.getClassName()))
                                .limit(6)
                                .forEach(classEntity -> results.add(new DashboardSearchDTO.Result(
                                                "class-" + classEntity.getClassId(),
                                                "Class",
                                                String.valueOf(classEntity.getClassId()),
                                                classEntity.getClassName(),
                                                "Academic class",
                                                studentsInClass(snapshot.students(), classEntity.getClassId()) + " active students",
                                                "pi pi-building",
                                                "/app/students/classes",
                                                "neutral",
                                                metadata("students", studentsInClass(snapshot.students(), classEntity.getClassId())))));

                snapshot.invoices().stream()
                                .filter(invoice -> matches(normalizedQuery, invoice.getInvoiceNumber(), String.valueOf(invoice.getStatus()), String.valueOf(invoice.getStudentId())))
                                .limit(6)
                                .forEach(invoice -> results.add(new DashboardSearchDTO.Result(
                                                "invoice-" + invoice.getId(),
                                                "Invoice",
                                                String.valueOf(invoice.getId()),
                                                invoice.getInvoiceNumber(),
                                                String.valueOf(invoice.getStatus()),
                                                money(invoice.getBalanceAmount()) + " balance due " + dateLabel(invoice.getDueDate()),
                                                "pi pi-wallet",
                                                null,
                                                invoiceTone(invoice),
                                                metadata("studentId", invoice.getStudentId(), "balance", invoice.getBalanceAmount()))));

                snapshot.applications().stream()
                                .filter(application -> matches(normalizedQuery, application.getApplicationId(), application.getApplicantName(), application.getEmail(), String.valueOf(application.getStatus())))
                                .limit(6)
                                .forEach(application -> results.add(new DashboardSearchDTO.Result(
                                                "admission-" + application.getApplicationId(),
                                                "Admission",
                                                application.getApplicationId(),
                                                application.getApplicantName(),
                                                String.valueOf(application.getStatus()),
                                                application.getApplyingForSchoolOrCollege(),
                                                "pi pi-user-plus",
                                                "/app/inquiry/applications",
                                                admissionTone(application.getStatus()),
                                                metadata("email", application.getEmail(), "contact", application.getContactNumber()))));

                snapshot.inquiries().stream()
                                .filter(inquiry -> matches(normalizedQuery, inquiry.getName(), inquiry.getEmail(), inquiry.getMobileNumber(), inquiry.getClassInterestedIn(), String.valueOf(inquiry.getStatus())))
                                .limit(6)
                                .forEach(inquiry -> results.add(new DashboardSearchDTO.Result(
                                                "inquiry-" + inquiry.getInquiryId(),
                                                "Inquiry",
                                                String.valueOf(inquiry.getInquiryId()),
                                                inquiry.getName(),
                                                String.valueOf(inquiry.getStatus()),
                                                "Next follow-up " + dateLabel(inquiry.getNextFollowUpDate()),
                                                "pi pi-comments",
                                                "/app/inquiry/pipeline",
                                                inquiryTone(inquiry),
                                                metadata("class", inquiry.getClassInterestedIn(), "mobile", inquiry.getMobileNumber()))));

                snapshot.todayClassAttendance().stream()
                                .filter(attendance -> matches(normalizedQuery, attendance.getReferenceName(), attendance.getClassName(), attendance.getSectionName(), String.valueOf(attendance.getStatus())))
                                .limit(6)
                                .forEach(attendance -> results.add(new DashboardSearchDTO.Result(
                                                "attendance-" + attendance.getId(),
                                                "Attendance",
                                                String.valueOf(attendance.getId()),
                                                attendance.getReferenceName(),
                                                String.valueOf(attendance.getStatus()),
                                                attendance.getClassName() + " " + nullToEmpty(attendance.getSectionName()),
                                                "pi pi-calendar-check",
                                                "/app/attendance/students",
                                                attendanceTone(attendance.getStatus()),
                                                metadata("date", attendance.getAttendanceDate(), "markedBy", attendance.getMarkedBy()))));

                branchRepository.findByOrganizationIdAndIsActive(context.organizationId(), Boolean.TRUE).stream()
                                .filter(branch -> matches(normalizedQuery, branch.getBranchName(), branch.getBranchCode(), branch.getLocation()))
                                .limit(4)
                                .forEach(branch -> results.add(new DashboardSearchDTO.Result(
                                                "branch-" + branch.getId(),
                                                "Branch",
                                                String.valueOf(branch.getId()),
                                                branch.getBranchName(),
                                                branch.getBranchCode(),
                                                branch.getLocation(),
                                                "pi pi-sitemap",
                                                "/app/admin/organizations",
                                                "success",
                                                metadata("location", branch.getLocation()))));

                departmentRepository.findByOrganizationIdAndIsActive(context.organizationId(), Boolean.TRUE).stream()
                                .filter(department -> matches(normalizedQuery, department.getDepartmentName(), department.getDepartmentCode(), department.getDescription()))
                                .limit(4)
                                .forEach(department -> results.add(new DashboardSearchDTO.Result(
                                                "department-" + department.getId(),
                                                "Department",
                                                String.valueOf(department.getId()),
                                                department.getDepartmentName(),
                                                department.getDepartmentCode(),
                                                department.getDescription(),
                                                "pi pi-objects-column",
                                                "/app/staff/operations",
                                                "neutral",
                                                metadata("code", department.getDepartmentCode()))));

                return new DashboardSearchDTO(query, results.stream().limit(30).toList(), SEARCH_CATEGORIES);
        }

        private DashboardWorkspaceDTO.UserContext userContext(
                        DashboardContext context,
                        DashboardSnapshot snapshot,
                        List<DashboardWorkspaceDTO.QuickAction> quickActions) {
                String focusMessage = quickActions.isEmpty()
                                ? "Review live school activity and alerts for your role."
                                : "Start with " + quickActions.get(0).label() + " or review the first priority below.";

                long alertCount = unresolvedEvents(snapshot.systemEvents()).size();
                if (alertCount > 0) {
                        focusMessage = alertCount + " alert" + plural(alertCount) + " need attention before routine work.";
                }

                return new DashboardWorkspaceDTO.UserContext(
                                context.userId(),
                                context.username(),
                                context.displayName(),
                                context.primaryRoleCode(),
                                context.primaryRoleName(),
                                context.roleCodes(),
                                context.organizationId(),
                                context.organizationName(),
                                context.tenantId(),
                                "Welcome back, " + firstName(context.displayName()),
                                focusMessage);
        }

        private List<DashboardWorkspaceDTO.Widget> widgets(List<DashboardWidgetConfig> configs) {
                return configs.stream()
                                .map(config -> new DashboardWorkspaceDTO.Widget(
                                                config.getWidgetKey(),
                                                config.getWidgetType(),
                                                config.getTitle(),
                                                config.getSubtitle(),
                                                config.getIcon(),
                                                config.getRoute(),
                                                config.getSectionKey(),
                                                config.getDisplayOrder()))
                                .toList();
        }

        private List<DashboardWorkspaceDTO.QuickAction> quickActions(List<DashboardWidgetConfig> configs) {
                return section(configs, "QUICK_ACTION").stream()
                                .limit(8)
                                .map(config -> new DashboardWorkspaceDTO.QuickAction(
                                                config.getWidgetKey(),
                                                config.getTitle(),
                                                config.getSubtitle(),
                                                config.getIcon(),
                                                config.getRoute(),
                                                actionTone(config.getWidgetKey()),
                                                true))
                                .toList();
        }

        private List<DashboardWorkspaceDTO.Kpi> kpis(List<DashboardWidgetConfig> configs, DashboardSnapshot snapshot) {
                Map<String, DashboardWorkspaceDTO.Kpi> candidates = kpiCandidates(snapshot);
                List<DashboardWorkspaceDTO.Kpi> configured = section(configs, "KPI").stream()
                                .map(config -> candidates.get(config.getWidgetKey()))
                                .filter(Objects::nonNull)
                                .limit(6)
                                .toList();

                return configured.isEmpty()
                                ? candidates.values().stream().limit(4).toList()
                                : configured;
        }

        private Map<String, DashboardWorkspaceDTO.Kpi> kpiCandidates(DashboardSnapshot snapshot) {
                long totalStudents = snapshot.students().size();
                long activeStudents = snapshot.students().stream().filter(student -> Boolean.TRUE.equals(student.getIsActive())).count();
                long activeStaff = snapshot.staff().stream().filter(staffMember -> Boolean.TRUE.equals(staffMember.getIsActive())).count();
                long openInquiries = snapshot.inquiries().stream().filter(this::isOpenInquiry).count();
                long dueFollowUps = snapshot.inquiries().stream().filter(this::isDueFollowUp).count();
                long pendingAdmissions = snapshot.applications().stream().filter(this::needsAdmissionAction).count();
                long pendingLeaves = snapshot.leaves().stream().filter(leave -> leave.getStatus() == LeaveStatus.PENDING).count();
                long overdueInvoices = snapshot.invoices().stream().filter(this::isOverdueInvoice).count();
                long unpaidInvoices = snapshot.invoices().stream().filter(this::isUnpaidInvoice).count();
                long activeUsers = safe(organizationUserRepository.countActiveUsersInOrganization(snapshot.organizationId()));
                long payrollProfiles = snapshot.payroll().size();
                long alerts = unresolvedEvents(snapshot.systemEvents()).size();
                AttendanceStats attendanceStats = attendanceStats(snapshot.todayClassAttendance());
                AttendanceStats staffAttendanceStats = attendanceStats(snapshot.todayStaffAttendance());

                Map<String, DashboardWorkspaceDTO.Kpi> candidates = new LinkedHashMap<>();
                candidates.put("school_health", kpi("school_health", "School Health", healthScore(snapshot) + "%", alerts == 0 ? "No blocking alerts" : alerts + " open alert" + plural(alerts), alerts == 0 ? "success" : "warning", "pi pi-shield"));
                candidates.put("active_users", kpi("active_users", "Active Users", String.valueOf(activeUsers), "Users mapped to this organization", "info", "pi pi-users"));
                candidates.put("total_students", kpi("total_students", "Students", String.valueOf(totalStudents), activeStudents + " active profiles", "success", "pi pi-user"));
                candidates.put("attendance_today", kpi("attendance_today", "Attendance Today", attendanceStats.rateLabel(), attendanceStats.helper(), attendanceStats.tone(), "pi pi-calendar-check"));
                candidates.put("class_absences", kpi("class_absences", "Absences", String.valueOf(attendanceStats.absentCount()), "Students absent or excused today", attendanceStats.absentCount() == 0 ? "success" : "warning", "pi pi-exclamation-circle"));
                candidates.put("open_inquiries", kpi("open_inquiries", "Open Inquiries", String.valueOf(openInquiries), dueFollowUps + " follow-up" + plural(dueFollowUps) + " due", dueFollowUps == 0 ? "info" : "warning", "pi pi-comments"));
                candidates.put("due_followups", kpi("due_followups", "Due Follow-ups", String.valueOf(dueFollowUps), "Admissions team queue", dueFollowUps == 0 ? "success" : "warning", "pi pi-phone"));
                candidates.put("pending_admissions", kpi("pending_admissions", "Admissions", String.valueOf(pendingAdmissions), "Draft, pending or under review", pendingAdmissions == 0 ? "success" : "warning", "pi pi-user-plus"));
                candidates.put("active_staff", kpi("active_staff", "Active Staff", String.valueOf(activeStaff), snapshot.staff().size() + " total staff profiles", "info", "pi pi-briefcase"));
                candidates.put("staff_attendance", kpi("staff_attendance", "Staff Attendance", staffAttendanceStats.rateLabel(), staffAttendanceStats.helper(), staffAttendanceStats.tone(), "pi pi-id-card"));
                candidates.put("pending_leaves", kpi("pending_leaves", "Leave Requests", String.valueOf(pendingLeaves), "Waiting for approval", pendingLeaves == 0 ? "success" : "warning", "pi pi-calendar-times"));
                candidates.put("payroll_profiles", kpi("payroll_profiles", "Payroll Profiles", String.valueOf(payrollProfiles), "Salary profiles ready", "neutral", "pi pi-wallet"));
                candidates.put("unpaid_invoices", kpi("unpaid_invoices", "Unpaid Invoices", String.valueOf(unpaidInvoices), overdueInvoices + " overdue", overdueInvoices == 0 ? "info" : "danger", "pi pi-receipt"));
                candidates.put("overdue_invoices", kpi("overdue_invoices", "Overdue Fees", String.valueOf(overdueInvoices), "Needs finance follow-up", overdueInvoices == 0 ? "success" : "danger", "pi pi-wallet"));
                candidates.put("open_alerts", kpi("open_alerts", "Smart Alerts", String.valueOf(alerts), "Unresolved operational alerts", alerts == 0 ? "success" : "warning", "pi pi-bell"));
                return candidates;
        }

        private DashboardWorkspaceDTO.Kpi kpi(String key, String label, String value, String helper, String tone, String icon) {
                return new DashboardWorkspaceDTO.Kpi(key, label, value, helper, tone, icon, null);
        }

        private List<DashboardWorkspaceDTO.Priority> priorities(
                        DashboardContext context,
                        DashboardSnapshot snapshot,
                        List<DashboardWorkspaceDTO.QuickAction> quickActions) {
                List<DashboardWorkspaceDTO.Priority> priorities = new ArrayList<>();
                String role = context.primaryRoleCode();

                if (isAny(role, "SUPER_ADMIN", "ADMIN", "PRINCIPAL")) {
                        long absences = attendanceStats(snapshot.todayClassAttendance()).absentCount();
                        addIf(priorities, absences > 0, "attendance-absence", absences + " student absence" + plural(absences),
                                        "Review today's attendance exceptions and parent communication needs.", "Today", "warning", "pi pi-calendar-times", "/app/attendance/students", "Attendance", null);
                        long pendingAdmissions = snapshot.applications().stream().filter(this::needsAdmissionAction).count();
                        addIf(priorities, pendingAdmissions > 0, "admissions-review", pendingAdmissions + " admission file" + plural(pendingAdmissions),
                                        "Clear pending or under-review applications before end of day.", "Today", "info", "pi pi-user-plus", "/app/inquiry/applications", "Admission", null);
                }

                if (isAny(role, "TEACHER")) {
                        List<Attendance> teacherAttendance = snapshot.todayClassAttendance().stream()
                                        .filter(attendance -> context.username().equalsIgnoreCase(nullToEmpty(attendance.getMarkedBy())))
                                        .toList();
                        addIf(priorities, teacherAttendance.isEmpty(), "teacher-attendance", "Mark class attendance",
                                        "No class attendance has been marked by you today.", "Today", "warning", "pi pi-calendar-plus", "/app/attendance/students", "Attendance", null);
                        snapshot.todayClassAttendance().stream()
                                        .filter(attendance -> attendance.getStatus() == AttendanceStatus.ABSENT || attendance.getStatus() == AttendanceStatus.LATE)
                                        .findFirst()
                                        .ifPresent(attendance -> priorities.add(new DashboardWorkspaceDTO.Priority(
                                                        "teacher-exception-" + attendance.getId(),
                                                        attendance.getReferenceName() + " needs follow-up",
                                                        attendance.getClassName() + " marked " + attendance.getStatus(),
                                                        "Today",
                                                        attendance.getStatus() == AttendanceStatus.ABSENT ? "danger" : "warning",
                                                        "pi pi-user-minus",
                                                        "/app/attendance/students",
                                                        "Attendance",
                                                        String.valueOf(attendance.getId()))));
                }

                if (isAny(role, "HR_MANAGER", "ADMIN", "PRINCIPAL")) {
                        long pendingLeaves = snapshot.leaves().stream().filter(leave -> leave.getStatus() == LeaveStatus.PENDING).count();
                        addIf(priorities, pendingLeaves > 0, "leave-approvals", pendingLeaves + " leave approval" + plural(pendingLeaves),
                                        "Approve or reject pending staff leave requests.", "Today", "warning", "pi pi-calendar-times", "/app/staff/operations", "Leave", null);
                }

                if (isAny(role, "ACCOUNTANT", "ADMIN", "PRINCIPAL")) {
                        long overdueInvoices = snapshot.invoices().stream().filter(this::isOverdueInvoice).count();
                        addIf(priorities, overdueInvoices > 0, "fee-overdue", overdueInvoices + " overdue invoice" + plural(overdueInvoices),
                                        "Follow up on overdue balances and payment reminders.", "This week", "danger", "pi pi-wallet", null, "Invoice", null);
                }

                if (isAny(role, "RECEPTIONIST", "COUNSELLOR")) {
                        long followUps = snapshot.inquiries().stream().filter(this::isDueFollowUp).count();
                        addIf(priorities, followUps > 0, "frontdesk-followups", followUps + " family follow-up" + plural(followUps),
                                        "Call or message due admission inquiries.", "Today", "warning", "pi pi-phone", "/app/inquiry/follow-ups", "Inquiry", null);
                }

                if (isAny(role, "PARENT")) {
                        List<Student> children = childrenForUser(snapshot.students(), context.user());
                        long childAbsences = snapshot.todayClassAttendance().stream()
                                        .filter(attendance -> children.stream().anyMatch(student -> Objects.equals(student.getStudentId(), attendance.getReferenceId())))
                                        .filter(attendance -> attendance.getStatus() == AttendanceStatus.ABSENT || attendance.getStatus() == AttendanceStatus.LATE)
                                        .count();
                        addIf(priorities, childAbsences > 0, "parent-attendance", childAbsences + " attendance update" + plural(childAbsences),
                                        "Review your child's latest attendance status.", "Today", "warning", "pi pi-calendar-check", null, "Attendance", null);
                }

                if (isAny(role, "STUDENT")) {
                        studentForUser(snapshot.students(), context.user()).ifPresent(student -> priorities.add(new DashboardWorkspaceDTO.Priority(
                                        "student-profile-" + student.getStudentId(),
                                        "Review today's school updates",
                                        classLabel(student) + " profile is active.",
                                        "Today",
                                        "info",
                                        "pi pi-book",
                                        null,
                                        "Student",
                                        String.valueOf(student.getStudentId()))));
                }

                if (priorities.isEmpty() && !quickActions.isEmpty()) {
                        DashboardWorkspaceDTO.QuickAction action = quickActions.get(0);
                        priorities.add(new DashboardWorkspaceDTO.Priority(
                                        "next-action-" + action.key(),
                                        action.label(),
                                        action.description(),
                                        "Now",
                                        action.tone(),
                                        action.icon(),
                                        action.route(),
                                        "Action",
                                        action.key()));
                }

                return priorities.stream().limit(5).toList();
        }

        private List<DashboardWorkspaceDTO.Approval> approvals(DashboardContext context, DashboardSnapshot snapshot) {
                String role = context.primaryRoleCode();
                List<DashboardWorkspaceDTO.Approval> approvals = new ArrayList<>();

                if (isAny(role, "SUPER_ADMIN", "ADMIN", "PRINCIPAL", "HR_MANAGER")) {
                        snapshot.leaves().stream()
                                        .filter(leave -> leave.getStatus() == LeaveStatus.PENDING)
                                        .limit(4)
                                        .forEach(leave -> approvals.add(new DashboardWorkspaceDTO.Approval(
                                                        "leave-" + leave.getId(),
                                                        leave.getStaffName(),
                                                        leave.getLeaveType() + " leave for " + leave.getDays() + " day" + plural(safeLong(leave.getDays())),
                                                        leave.getAppliedBy(),
                                                        String.valueOf(leave.getStatus()),
                                                        "warning",
                                                        "/app/staff/operations",
                                                        "Leave",
                                                        String.valueOf(leave.getId()))));
                }

                if (isAny(role, "SUPER_ADMIN", "ADMIN", "PRINCIPAL", "RECEPTIONIST", "COUNSELLOR")) {
                        snapshot.applications().stream()
                                        .filter(this::needsAdmissionAction)
                                        .limit(4)
                                        .forEach(application -> approvals.add(new DashboardWorkspaceDTO.Approval(
                                                        "admission-" + application.getApplicationId(),
                                                        application.getApplicantName(),
                                                        application.getApplyingForSchoolOrCollege(),
                                                        application.getParentName(),
                                                        String.valueOf(application.getStatus()),
                                                        admissionTone(application.getStatus()),
                                                        "/app/inquiry/applications",
                                                        "Admission",
                                                        application.getApplicationId())));
                }

                if (isAny(role, "ACCOUNTANT", "SUPER_ADMIN", "ADMIN", "PRINCIPAL")) {
                        snapshot.invoices().stream()
                                        .filter(this::isOverdueInvoice)
                                        .limit(3)
                                        .forEach(invoice -> approvals.add(new DashboardWorkspaceDTO.Approval(
                                                        "invoice-" + invoice.getId(),
                                                        invoice.getInvoiceNumber(),
                                                        money(invoice.getBalanceAmount()) + " balance due " + dateLabel(invoice.getDueDate()),
                                                        "Finance",
                                                        String.valueOf(invoice.getStatus()),
                                                        invoiceTone(invoice),
                                                        null,
                                                        "Invoice",
                                                        String.valueOf(invoice.getId()))));
                }

                return approvals.stream().limit(6).toList();
        }

        private List<DashboardWorkspaceDTO.Activity> activities(DashboardSnapshot snapshot) {
                List<DashboardWorkspaceDTO.Activity> activities = new ArrayList<>();
                snapshot.auditLogs().stream().limit(8).forEach(audit -> activities.add(new DashboardWorkspaceDTO.Activity(
                                "audit-" + audit.getId(),
                                audit.getAction(),
                                nullToDefault(audit.getSummary(), nullToDefault(audit.getEntityType(), "School activity")),
                                nullToDefault(audit.getActorUsername(), "System"),
                                audit.getOccurredAt(),
                                auditTone(String.valueOf(audit.getEventType())),
                                "pi pi-history",
                                routeForEntity(audit.getEntityType()))));

                if (activities.size() < 6) {
                        snapshot.notifications().stream().limit(6 - activities.size()).forEach(notification -> activities.add(new DashboardWorkspaceDTO.Activity(
                                        "notification-" + notification.getId(),
                                        notification.getSubject(),
                                        notification.getCategory(),
                                        "Notification Center",
                                        notification.getSentAt() != null ? notification.getSentAt() : notification.getScheduledAt(),
                                        severityTone(String.valueOf(notification.getSeverity())),
                                        "pi pi-send",
                                        null)));
                }

                return activities;
        }

        private List<DashboardWorkspaceDTO.Alert> alerts(DashboardSnapshot snapshot) {
                List<DashboardWorkspaceDTO.Alert> alerts = new ArrayList<>();
                unresolvedEvents(snapshot.systemEvents()).stream().limit(4).forEach(event -> alerts.add(new DashboardWorkspaceDTO.Alert(
                                "event-" + event.getId(),
                                event.getTitle(),
                                event.getMessage(),
                                event.getSeverity(),
                                severityTone(event.getSeverity()),
                                iconForEvent(event.getCategory()),
                                "/app/admin/monitoring",
                                "SystemEvent",
                                String.valueOf(event.getId()))));

                long absences = attendanceStats(snapshot.todayClassAttendance()).absentCount();
                addAlert(alerts, absences > 0, "attendance-alert", "Attendance exceptions", absences + " student attendance exception" + plural(absences) + " today.", "MEDIUM", "warning", "pi pi-calendar-times", "/app/attendance/students", "Attendance", null);

                long overdueInvoices = snapshot.invoices().stream().filter(this::isOverdueInvoice).count();
                addAlert(alerts, overdueInvoices > 0, "fee-alert", "Fee follow-up needed", overdueInvoices + " overdue invoice" + plural(overdueInvoices) + " require finance action.", "HIGH", "danger", "pi pi-wallet", null, "Invoice", null);

                long dueFollowups = snapshot.inquiries().stream().filter(this::isDueFollowUp).count();
                addAlert(alerts, dueFollowups > 0, "followup-alert", "Inquiry follow-ups due", dueFollowups + " family follow-up" + plural(dueFollowups) + " due today or overdue.", "MEDIUM", "warning", "pi pi-phone", "/app/inquiry/follow-ups", "Inquiry", null);

                return alerts.stream().limit(6).toList();
        }

        private List<DashboardWorkspaceDTO.Shortcut> shortcuts(List<DashboardWidgetConfig> configs, DashboardSnapshot snapshot) {
                return section(configs, "SHORTCUT").stream()
                                .limit(8)
                                .map(config -> new DashboardWorkspaceDTO.Shortcut(
                                                config.getWidgetKey(),
                                                config.getTitle(),
                                                config.getSubtitle(),
                                                config.getIcon(),
                                                config.getRoute(),
                                                countForShortcut(config.getWidgetKey(), snapshot),
                                                actionTone(config.getWidgetKey())))
                                .toList();
        }

        private List<DashboardWidgetConfig> visibleWidgetConfig(List<String> roleCodes) {
                LinkedHashSet<String> lookupRoles = new LinkedHashSet<>(roleCodes);
                lookupRoles.add(DEFAULT_ROLE);
                List<DashboardWidgetConfig> configs = widgetConfigRepository.findByRoleCodeInAndEnabledTrue(lookupRoles);

                Comparator<DashboardWidgetConfig> comparator = Comparator
                                .comparingInt((DashboardWidgetConfig config) -> roleRank(config.getRoleCode(), roleCodes))
                                .thenComparing(config -> Optional.ofNullable(config.getDisplayOrder()).orElse(999));

                Map<String, DashboardWidgetConfig> deduped = new LinkedHashMap<>();
                configs.stream().sorted(comparator).forEach(config ->
                                deduped.putIfAbsent(config.getSectionKey() + "::" + config.getWidgetKey(), config));
                return deduped.values().stream()
                                .sorted(Comparator.comparing(config -> Optional.ofNullable(config.getDisplayOrder()).orElse(999)))
                                .toList();
        }

        private int roleRank(String roleCode, List<String> currentRoles) {
                if (roleCode == null) {
                        return 50;
                }
                int index = currentRoles.indexOf(roleCode);
                if (index >= 0) {
                        return index;
                }
                return DEFAULT_ROLE.equals(roleCode) ? 40 : 20;
        }

        private List<DashboardWidgetConfig> section(List<DashboardWidgetConfig> configs, String sectionKey) {
                return configs.stream()
                                .filter(config -> sectionKey.equalsIgnoreCase(config.getSectionKey()))
                                .sorted(Comparator.comparing(config -> Optional.ofNullable(config.getDisplayOrder()).orElse(999)))
                                .toList();
        }

        private DashboardSnapshot snapshot(Long organizationId) {
                LocalDate today = LocalDate.now();
                List<Student> students = organizationId == null ? List.of() : studentRepository.findByOrganizationId(organizationId);
                List<Staff> staff = organizationId == null ? List.of() : staffRepository.findByOrganizationId(organizationId);
                List<Inquiry> inquiries = inquiryRepository.findAllByIsDeletedFalseOrderByCreatedDateDesc().stream()
                                .filter(inquiry -> matchesOrg(inquiry.getOrganizationId(), organizationId))
                                .toList();
                List<ApplicationAdmission> applications = applicationAdmissionRepository.findAll().stream()
                                .filter(application -> matchesOrg(application.getOrganizationId(), organizationId))
                                .toList();
                List<FeeInvoice> invoices = feeInvoiceRepository.findAll().stream()
                                .filter(invoice -> matchesOrg(invoice.getOrganizationId(), organizationId))
                                .toList();
                List<Attendance> todayClassAttendance = organizationId == null ? List.of()
                                : attendanceRepository.findByOrganizationIdAndAttendanceDateAndAttendanceType(organizationId, today, AttendanceType.CLASS);
                List<Attendance> todayStaffAttendance = organizationId == null ? List.of()
                                : attendanceRepository.findByOrganizationIdAndAttendanceDateAndAttendanceType(organizationId, today, AttendanceType.STAFF);
                List<LeaveRequest> leaves = organizationId == null ? List.of()
                                : leaveRepository.findByOrganizationIdOrderByCreatedDateDesc(organizationId);
                List<StaffPayroll> payroll = organizationId == null ? List.of() : payrollRepository.findByOrganizationId(organizationId);
                List<AuditLog> auditLogs = organizationId == null ? List.of()
                                : auditLogRepository.findByOrganizationId(organizationId,
                                                PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "occurredAt"))).getContent();
                List<SystemEvent> systemEvents = organizationId == null ? systemEventRepository.findTop50ByOrderByOccurredAtDesc()
                                : systemEventRepository.findTop50ByOrganizationIdOrderByOccurredAtDesc(organizationId);
                List<Notification> notifications = organizationId == null ? List.of()
                                : notificationRepository.findByOrganizationId(organizationId,
                                                PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
                List<WorkflowConfig> workflows = organizationId == null ? List.of() : workflowConfigRepository.findByOrganizationId(organizationId);
                List<ClassEntity> classes = organizationId == null ? List.of() : classRepository.findByOrganizationId(organizationId);
                List<Guardian> guardians = guardianRepository.findAll();

                return new DashboardSnapshot(
                                organizationId,
                                students,
                                staff,
                                inquiries,
                                applications,
                                invoices,
                                todayClassAttendance,
                                todayStaffAttendance,
                                leaves,
                                payroll,
                                auditLogs,
                                systemEvents,
                                notifications,
                                workflows,
                                classes,
                                guardians);
        }

        private DashboardContext currentDashboardContext() {
                String username = Optional.ofNullable(SecurityUtil.getCurrentUsername()).orElse("system");
                User user = userRepository.findByUserName(username)
                                .or(() -> userRepository.findByEmail(username))
                                .orElse(null);
                List<String> roleCodes = roleCodes(user);
                String primaryRole = roleCodes.isEmpty() ? "STAFF" : roleCodes.get(0);
                String primaryRoleName = primaryRoleName(user, primaryRole);
                Long organizationId = currentOrgId();
                if (organizationId == null && user != null && user.getOrganizations() != null && !user.getOrganizations().isEmpty()) {
                        organizationId = user.getOrganizations().get(0).getOrgId();
                }
                if (organizationId == null) {
                        organizationId = DEFAULT_ORG_ID;
                }
                Organisation organization = organizationRepository.findById(organizationId).orElse(null);
                String organizationName = organization != null ? organization.getOrgName() : "Current Organization";
                String tenantId = Optional.ofNullable(TenantContext.getTenant()).orElse("public");
                String displayName = user == null ? username : fullName(user.getFirstName(), user.getMiddleName(), user.getLastName());

                return new DashboardContext(
                                user,
                                user == null ? null : user.getId(),
                                username,
                                displayName,
                                roleCodes.isEmpty() ? List.of(primaryRole) : roleCodes,
                                primaryRole,
                                primaryRoleName,
                                organizationId,
                                organizationName,
                                tenantId);
        }

        private List<String> roleCodes(User user) {
                LinkedHashSet<String> roleCodes = new LinkedHashSet<>();
                if (user != null && user.getRoles() != null) {
                        user.getRoles().stream()
                                        .map(Role::getRoleCode)
                                        .filter(Objects::nonNull)
                                        .map(this::stripRolePrefix)
                                        .forEach(roleCodes::add);
                }

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null) {
                        authentication.getAuthorities().stream()
                                        .map(authority -> stripRolePrefix(authority.getAuthority()))
                                        .filter(KNOWN_ROLES::contains)
                                        .forEach(roleCodes::add);
                }
                return new ArrayList<>(roleCodes);
        }

        private String primaryRoleName(User user, String primaryRole) {
                if (user != null && user.getRoles() != null) {
                        return user.getRoles().stream()
                                        .filter(role -> primaryRole.equals(stripRolePrefix(role.getRoleCode())))
                                        .map(Role::getRoleName)
                                        .findFirst()
                                        .orElse(titleCase(primaryRole));
                }
                return titleCase(primaryRole);
        }

        private String stripRolePrefix(String value) {
                return nullToEmpty(value).replaceFirst("^ROLE_", "").trim();
        }

        private boolean matchesOrg(Long rowOrganizationId, Long organizationId) {
                return organizationId == null || rowOrganizationId == null || Objects.equals(rowOrganizationId, organizationId);
        }

        private boolean isOpenInquiry(Inquiry inquiry) {
                return inquiry.getStatus() != InquiryStatus.CONVERTED
                                && inquiry.getStatus() != InquiryStatus.LOST
                                && inquiry.getStatus() != InquiryStatus.CLOSED;
        }

        private boolean isDueFollowUp(Inquiry inquiry) {
                return inquiry.getNextFollowUpDate() != null
                                && !inquiry.getNextFollowUpDate().isAfter(LocalDate.now())
                                && isOpenInquiry(inquiry);
        }

        private boolean needsAdmissionAction(ApplicationAdmission application) {
                return application.getStatus() == ApplicationStatus.PENDING
                                || application.getStatus() == ApplicationStatus.UNDER_REVIEW
                                || application.getStatus() == ApplicationStatus.DRAFT;
        }

        private boolean isUnpaidInvoice(FeeInvoice invoice) {
                return invoice.getStatus() == InvoiceStatus.ISSUED
                                || invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID
                                || invoice.getStatus() == InvoiceStatus.OVERDUE
                                || positive(invoice.getBalanceAmount());
        }

        private boolean isOverdueInvoice(FeeInvoice invoice) {
                return invoice.getStatus() == InvoiceStatus.OVERDUE
                                || (invoice.getDueDate() != null && invoice.getDueDate().isBefore(LocalDate.now()) && positive(invoice.getBalanceAmount()));
        }

        private boolean positive(BigDecimal value) {
                return value != null && value.compareTo(BigDecimal.ZERO) > 0;
        }

        private List<SystemEvent> unresolvedEvents(List<SystemEvent> events) {
                return events.stream()
                                .filter(event -> !Boolean.TRUE.equals(event.getResolved()))
                                .toList();
        }

        private AttendanceStats attendanceStats(List<Attendance> attendanceRows) {
                long total = attendanceRows.size();
                long present = attendanceRows.stream()
                                .filter(attendance -> attendance.getStatus() == AttendanceStatus.PRESENT || attendance.getStatus() == AttendanceStatus.LATE)
                                .count();
                long absent = attendanceRows.stream()
                                .filter(attendance -> attendance.getStatus() == AttendanceStatus.ABSENT
                                                || attendance.getStatus() == AttendanceStatus.EXCUSED
                                                || attendance.getStatus() == AttendanceStatus.ON_LEAVE)
                                .count();
                if (total == 0) {
                        return new AttendanceStats(0, 0, 0, "No attendance marked today", "warning");
                }
                long rate = Math.round((present * 100.0) / total);
                return new AttendanceStats(total, present, absent, present + " of " + total + " marked present", rate >= 90 ? "success" : rate >= 75 ? "warning" : "danger");
        }

        private long healthScore(DashboardSnapshot snapshot) {
                long unresolved = unresolvedEvents(snapshot.systemEvents()).size();
                long overdueInvoices = snapshot.invoices().stream().filter(this::isOverdueInvoice).count();
                long absences = attendanceStats(snapshot.todayClassAttendance()).absentCount();
                long score = 100 - (unresolved * 6) - Math.min(20, overdueInvoices * 2) - Math.min(15, absences);
                return Math.max(65, Math.min(100, score));
        }

        private Long countForShortcut(String key, DashboardSnapshot snapshot) {
                return switch (key) {
                        case "students", "student_profiles", "children" -> (long) snapshot.students().size();
                        case "staff", "hr_staff" -> (long) snapshot.staff().size();
                        case "attendance", "teacher_attendance" -> (long) snapshot.todayClassAttendance().size();
                        case "admissions", "frontdesk_admissions" -> snapshot.applications().stream().filter(this::needsAdmissionAction).count();
                        case "inquiries", "frontdesk_inquiries" -> snapshot.inquiries().stream().filter(this::isOpenInquiry).count();
                        case "fees", "finance_invoices" -> snapshot.invoices().stream().filter(this::isUnpaidInvoice).count();
                        case "approvals", "hr_leave" -> snapshot.leaves().stream().filter(leave -> leave.getStatus() == LeaveStatus.PENDING).count();
                        case "alerts", "monitoring" -> (long) unresolvedEvents(snapshot.systemEvents()).size();
                        default -> null;
                };
        }

        private List<Student> childrenForUser(List<Student> students, User user) {
                if (user == null) {
                        return List.of();
                }
                String userEmail = normalize(user.getEmail());
                return students.stream()
                                .filter(student -> student.getParent() != null && normalize(student.getParent().getEmail()).equals(userEmail))
                                .toList();
        }

        private Optional<Student> studentForUser(List<Student> students, User user) {
                if (user == null) {
                        return Optional.empty();
                }
                return students.stream()
                                .filter(student -> student.getUser() != null && Objects.equals(student.getUser().getId(), user.getId()))
                                .findFirst()
                                .or(() -> students.stream().filter(student -> normalize(student.getEmail()).equals(normalize(user.getEmail()))).findFirst());
        }

        private long studentsInClass(List<Student> students, Long classId) {
                return students.stream()
                                .filter(student -> student.getClassEntity() != null && Objects.equals(student.getClassEntity().getClassId(), classId))
                                .count();
        }

        private void addIf(
                        List<DashboardWorkspaceDTO.Priority> priorities,
                        boolean condition,
                        String key,
                        String title,
                        String description,
                        String dueLabel,
                        String tone,
                        String icon,
                        String route,
                        String entityType,
                        String entityId) {
                if (condition) {
                        priorities.add(new DashboardWorkspaceDTO.Priority(key, title, description, dueLabel, tone, icon, route, entityType, entityId));
                }
        }

        private void addAlert(
                        List<DashboardWorkspaceDTO.Alert> alerts,
                        boolean condition,
                        String key,
                        String title,
                        String description,
                        String severity,
                        String tone,
                        String icon,
                        String route,
                        String entityType,
                        String entityId) {
                if (condition) {
                        alerts.add(new DashboardWorkspaceDTO.Alert(key, title, description, severity, tone, icon, route, entityType, entityId));
                }
        }

        private boolean isAny(String role, String... candidates) {
                for (String candidate : candidates) {
                        if (candidate.equals(role)) {
                                return true;
                        }
                }
                return false;
        }

        private String actionTone(String key) {
                if (key == null) {
                        return "neutral";
                }
                if (key.contains("alert") || key.contains("monitor")) {
                        return "warning";
                }
                if (key.contains("fee") || key.contains("invoice")) {
                        return "danger";
                }
                if (key.contains("attendance") || key.contains("student")) {
                        return "success";
                }
                if (key.contains("admission") || key.contains("inquiry")) {
                        return "info";
                }
                return "neutral";
        }

        private String severityTone(String severity) {
                String normalized = normalize(severity);
                if (normalized.contains("high") || normalized.contains("critical") || normalized.contains("danger")) {
                        return "danger";
                }
                if (normalized.contains("medium") || normalized.contains("warn")) {
                        return "warning";
                }
                if (normalized.contains("low") || normalized.contains("info")) {
                        return "info";
                }
                return "neutral";
        }

        private String invoiceTone(FeeInvoice invoice) {
                if (isOverdueInvoice(invoice)) {
                        return "danger";
                }
                return isUnpaidInvoice(invoice) ? "warning" : "success";
        }

        private String admissionTone(ApplicationStatus status) {
                if (status == ApplicationStatus.REJECTED) {
                        return "danger";
                }
                if (status == ApplicationStatus.APPROVED) {
                        return "success";
                }
                return "warning";
        }

        private String inquiryTone(Inquiry inquiry) {
                return isDueFollowUp(inquiry) ? "warning" : "info";
        }

        private String attendanceTone(AttendanceStatus status) {
                if (status == AttendanceStatus.ABSENT || status == AttendanceStatus.ON_LEAVE) {
                        return "danger";
                }
                if (status == AttendanceStatus.LATE || status == AttendanceStatus.EXCUSED) {
                        return "warning";
                }
                return "success";
        }

        private String auditTone(String eventType) {
                String normalized = normalize(eventType);
                if (normalized.contains("delete") || normalized.contains("reject")) {
                        return "danger";
                }
                if (normalized.contains("approve") || normalized.contains("create")) {
                        return "success";
                }
                if (normalized.contains("config") || normalized.contains("update")) {
                        return "warning";
                }
                return "info";
        }

        private String iconForEvent(String category) {
                String normalized = normalize(category);
                if (normalized.contains("security")) {
                        return "pi pi-lock";
                }
                if (normalized.contains("notification")) {
                        return "pi pi-bell";
                }
                if (normalized.contains("job")) {
                        return "pi pi-cog";
                }
                return "pi pi-exclamation-triangle";
        }

        private String routeForEntity(String entityType) {
                String normalized = normalize(entityType);
                if (normalized.contains("student")) {
                        return "/app/students/directory";
                }
                if (normalized.contains("branch") || normalized.contains("organization")) {
                        return "/app/admin/organizations";
                }
                if (normalized.contains("role") || normalized.contains("user")) {
                        return "/app/admin/access";
                }
                if (normalized.contains("attendance")) {
                        return "/app/attendance/dashboard";
                }
                return null;
        }

        private String classLabel(Student student) {
                if (student.getClassEntity() == null) {
                        return "Class not assigned";
                }
                String section = student.getSection() == null ? "" : " · " + student.getSection().getSectionName();
                return student.getClassEntity().getClassName() + section;
        }

        private String departmentLabel(Staff staffMember) {
                return staffMember.getDepartment() == null ? "Department not assigned" : staffMember.getDepartment().getDepartmentName();
        }

        private String fullName(String firstName, String middleName, String lastName) {
                return List.of(nullToEmpty(firstName), nullToEmpty(middleName), nullToEmpty(lastName)).stream()
                                .filter(value -> !value.isBlank())
                                .collect(Collectors.joining(" "));
        }

        private String firstName(String displayName) {
                String trimmed = nullToEmpty(displayName).trim();
                if (trimmed.isEmpty()) {
                        return "there";
                }
                return trimmed.split("\\s+")[0];
        }

        private String titleCase(String value) {
                return List.of(nullToEmpty(value).split("_"))
                                .stream()
                                .filter(part -> !part.isBlank())
                                .map(part -> part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1).toLowerCase(Locale.ROOT))
                                .collect(Collectors.joining(" "));
        }

        private String normalize(String value) {
                return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
        }

        private boolean matches(String normalizedQuery, String... values) {
                for (String value : values) {
                        if (normalize(value).contains(normalizedQuery)) {
                                return true;
                        }
                }
                return false;
        }

        private String nullToEmpty(String value) {
                return value == null ? "" : value;
        }

        private String nullToDefault(String value, String fallback) {
                return value == null || value.isBlank() ? fallback : value;
        }

        private String dateLabel(LocalDate date) {
                return date == null ? "not scheduled" : "on " + date;
        }

        private String money(BigDecimal value) {
                return value == null ? "0" : value.stripTrailingZeros().toPlainString();
        }

        private String plural(long count) {
                return count == 1 ? "" : "s";
        }

        private long safeLong(Integer value) {
                return value == null ? 0L : value.longValue();
        }

        private String tone(boolean positive) {
                return positive ? "success" : "warning";
        }

        private Map<String, Object> metadata(Object... pairs) {
                Map<String, Object> metadata = new LinkedHashMap<>();
                for (int index = 0; index + 1 < pairs.length; index += 2) {
                        Object value = pairs[index + 1];
                        if (pairs[index] != null && value != null) {
                                metadata.put(String.valueOf(pairs[index]), value);
                        }
                }
                return metadata;
        }

        private Long currentOrgId() {
                return OrganizationContext.getOrganizationId();
        }

        private static long safe(Long value) {
                return value == null ? 0L : value;
        }

        private record DashboardContext(
                        User user,
                        Long userId,
                        String username,
                        String displayName,
                        List<String> roleCodes,
                        String primaryRoleCode,
                        String primaryRoleName,
                        Long organizationId,
                        String organizationName,
                        String tenantId) {
        }

        private record DashboardSnapshot(
                        Long organizationId,
                        List<Student> students,
                        List<Staff> staff,
                        List<Inquiry> inquiries,
                        List<ApplicationAdmission> applications,
                        List<FeeInvoice> invoices,
                        List<Attendance> todayClassAttendance,
                        List<Attendance> todayStaffAttendance,
                        List<LeaveRequest> leaves,
                        List<StaffPayroll> payroll,
                        List<AuditLog> auditLogs,
                        List<SystemEvent> systemEvents,
                        List<Notification> notifications,
                        List<WorkflowConfig> workflows,
                        List<ClassEntity> classes,
                        List<Guardian> guardians) {
        }

        private record AttendanceStats(long totalCount, long presentCount, long absentCount, String helper, String tone) {
                private String rateLabel() {
                        if (totalCount == 0) {
                                return "0%";
                        }
                        return Math.round((presentCount * 100.0) / totalCount) + "%";
                }
        }
}
