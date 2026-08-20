package com.thinkerscave.dashboard.service;

import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.access.entity.Menu;
import com.thinkerscave.access.repository.MenuRepository;
import com.thinkerscave.admission.entity.Inquiry;
import com.thinkerscave.admission.repository.InquiryRepository;
import com.thinkerscave.dashboard.dto.DashboardSearchResponseDTO;
import com.thinkerscave.dashboard.dto.DashboardSearchResultDTO;
import com.thinkerscave.platform.entity.Customer;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.repository.CustomerRepository;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardSearchService {

    private static final int LIMIT = 8;
    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);

    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final InquiryRepository inquiryRepository;
    private final ClassRepository classRepository;
    private final CustomerRepository customerRepository;
    private final OrganizationRepository organizationRepository;
    private final MenuRepository menuRepository;

    public DashboardSearchResponseDTO search(String query) {
        String normalized = query == null ? "" : query.trim();
        SearchScope scope = resolveScope();
        if (normalized.length() < 2 || scope == SearchScope.NONE) {
            return DashboardSearchResponseDTO.builder()
                    .query(normalized)
                    .results(List.of())
                    .supportedCategories(scope.categories())
                    .build();
        }

        List<DashboardSearchResultDTO> results = new ArrayList<>();
        switch (scope) {
            case PLATFORM -> {
                results.addAll(safe("customers", () -> searchCustomers(normalized)));
                results.addAll(safe("organizations", () -> searchOrganizations(normalized)));
                results.addAll(safe("menus", () -> searchMenus(normalized)));
            }
            case ORGANIZATION -> {
                results.addAll(safe("students", () -> searchStudents(normalized)));
                results.addAll(safe("staff", () -> searchStaff(normalized)));
                results.addAll(safe("classes", () -> searchClasses(normalized)));
                results.addAll(safe("leads", () -> searchLeads(normalized)));
            }
            case TEACHER -> results.addAll(safe("students", () -> searchStudents(normalized)));
            case NONE -> {
            }
        }

        return DashboardSearchResponseDTO.builder()
                .query(normalized)
                .results(results.stream().limit(32).toList())
                .supportedCategories(scope.categories())
                .build();
    }

    private SearchScope resolveScope() {
        if (hasAuthority("STUDENT", "PARENT")) {
            return SearchScope.NONE;
        }
        if (hasAuthority("SUPER_ADMIN", "PLATFORM_ADMIN", "THINKERSCAVE_INTERNAL", "INTERNAL_TEAM")
                || OrganizationContext.getOrganizationId() == null && hasAuthority("SUPER_ADMIN")) {
            return SearchScope.PLATFORM;
        }
        if (hasAuthority(
                "ORGANIZATION_ADMIN", "ORGANIZATION_OWNER", "INSTITUTION_ADMIN", "COLLEGE_ADMIN",
                "ADMIN", "PRINCIPAL", "HR_MANAGER", "ACADEMIC_COORDINATOR", "RECEPTIONIST")) {
            return SearchScope.ORGANIZATION;
        }
        if (hasAuthority("TEACHER", "STAFF")) {
            return SearchScope.TEACHER;
        }
        return OrganizationContext.getOrganizationId() == null ? SearchScope.PLATFORM : SearchScope.ORGANIZATION;
    }

    private List<DashboardSearchResultDTO> searchCustomers(String keyword) {
        return customerRepository.searchCustomers(
                        true, null, keyword, false, EPOCH, false, EPOCH, PageRequest.of(0, LIMIT))
                .stream()
                .map(this::toCustomerResult)
                .toList();
    }

    private List<DashboardSearchResultDTO> searchOrganizations(String keyword) {
        return organizationRepository.searchOrganizations(null, null, null, keyword, PageRequest.of(0, LIMIT))
                .stream()
                .map(this::toOrganizationResult)
                .toList();
    }

    private List<DashboardSearchResultDTO> searchMenus(String keyword) {
        return menuRepository.searchByKeyword(keyword, PageRequest.of(0, LIMIT)).stream()
                .filter(menu -> StringUtils.hasText(menu.getRoute()))
                .map(this::toMenuResult)
                .toList();
    }

    private List<DashboardSearchResultDTO> searchStudents(String keyword) {
        return studentRepository.searchByKeyword(keyword, PageRequest.of(0, LIMIT))
                .stream()
                .map(this::toStudentResult)
                .toList();
    }

    private List<DashboardSearchResultDTO> searchStaff(String keyword) {
        return staffRepository.searchStaff(null, null, null, null, keyword, PageRequest.of(0, LIMIT))
                .stream()
                .map(this::toStaffResult)
                .toList();
    }

    private List<DashboardSearchResultDTO> searchClasses(String keyword) {
        return classRepository.searchByKeyword(keyword, PageRequest.of(0, LIMIT)).stream()
                .map(this::toClassResult)
                .toList();
    }

    private List<DashboardSearchResultDTO> searchLeads(String keyword) {
        if (OrganizationContext.getOrganizationId() == null) {
            return List.of();
        }
        return inquiryRepository.search(keyword, PageRequest.of(0, LIMIT))
                .stream()
                .map(this::toLeadResult)
                .toList();
    }

    private DashboardSearchResultDTO toCustomerResult(Customer customer) {
        return DashboardSearchResultDTO.builder()
                .key("customer-" + customer.getId())
                .entityType("Customer")
                .entityId(String.valueOf(customer.getId()))
                .title(customer.getCustomerName())
                .subtitle(customer.getCustomerCode())
                .detail(customer.getBusinessEmail())
                .icon("pi pi-briefcase")
                .route("/app/tenant-management/customers/" + customer.getId())
                .tone("primary")
                .build();
    }

    private DashboardSearchResultDTO toOrganizationResult(Organization organization) {
        return DashboardSearchResultDTO.builder()
                .key("organization-" + organization.getId())
                .entityType("Organization")
                .entityId(String.valueOf(organization.getId()))
                .title(organization.getOrganizationName())
                .subtitle(organization.getOrganizationCode())
                .detail(organization.getStatus() != null ? organization.getStatus().name() : null)
                .icon("pi pi-building")
                .route("/app/tenant-management/organizations/" + organization.getId())
                .tone("info")
                .build();
    }

    private DashboardSearchResultDTO toMenuResult(Menu menu) {
        return DashboardSearchResultDTO.builder()
                .key("menu-" + menu.getId())
                .entityType("Menu")
                .entityId(String.valueOf(menu.getId()))
                .title(menu.getMenuName())
                .subtitle(menu.getMenuCode())
                .detail(menu.getDescription())
                .icon(StringUtils.hasText(menu.getIcon()) ? menu.getIcon() : "pi pi-sitemap")
                .route(menu.getRoute())
                .tone("secondary")
                .build();
    }

    private DashboardSearchResultDTO toStudentResult(Student student) {
        return DashboardSearchResultDTO.builder()
                .key("student-" + student.getStudentId())
                .entityType("Student")
                .entityId(String.valueOf(student.getStudentId()))
                .title(joinName(student.getFirstName(), student.getLastName()))
                .subtitle(student.getStudentCode())
                .detail(student.getAdmissionNumber())
                .icon("pi pi-user")
                .route("/app/students/profile/" + student.getStudentId())
                .tone("primary")
                .build();
    }

    private DashboardSearchResultDTO toStaffResult(Staff staff) {
        return DashboardSearchResultDTO.builder()
                .key("staff-" + staff.getStaffId())
                .entityType("Staff")
                .entityId(String.valueOf(staff.getStaffId()))
                .title(joinName(staff.getFirstName(), staff.getLastName()))
                .subtitle(staff.getStaffCode())
                .detail(staff.getDesignation())
                .icon("pi pi-briefcase")
                .route("/app/staff/profile/" + staff.getStaffId())
                .tone("info")
                .build();
    }

    private DashboardSearchResultDTO toClassResult(AcademicClass academicClass) {
        String yearName = academicClass.getAcademicYear() != null ? academicClass.getAcademicYear().getName() : null;
        return DashboardSearchResultDTO.builder()
                .key("class-" + academicClass.getClassId())
                .entityType("Class")
                .entityId(String.valueOf(academicClass.getClassId()))
                .title(academicClass.getName())
                .subtitle(academicClass.getCode())
                .detail(yearName)
                .icon("pi pi-th-large")
                .route("/app/academics/classes-sections/" + academicClass.getClassId())
                .tone("success")
                .build();
    }

    private DashboardSearchResultDTO toLeadResult(Inquiry inquiry) {
        return DashboardSearchResultDTO.builder()
                .key("lead-" + inquiry.getInquiryId())
                .entityType("Lead")
                .entityId(String.valueOf(inquiry.getInquiryId()))
                .title(inquiry.getName())
                .subtitle(inquiry.getMobileNumber())
                .detail(inquiry.getClassInterestedIn())
                .icon("pi pi-user-plus")
                .route("/app/admissions/lead/" + inquiry.getInquiryId())
                .tone("warning")
                .build();
    }

    private List<DashboardSearchResultDTO> safe(String source, Supplier<List<DashboardSearchResultDTO>> supplier) {
        try {
            List<DashboardSearchResultDTO> value = supplier.get();
            return value != null ? value : List.of();
        } catch (Exception ex) {
            log.warn("Global search skipped {}: {}", source, ex.getMessage());
            return List.of();
        }
    }

    private boolean hasAuthority(String... names) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        Set<String> granted = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        for (String name : names) {
            if (granted.contains(name) || granted.contains("ROLE_" + name)) {
                return true;
            }
        }
        return false;
    }

    private String joinName(String first, String last) {
        String combined = String.join(" ",
                StringUtils.hasText(first) ? first.trim() : "",
                StringUtils.hasText(last) ? last.trim() : "").trim();
        return combined.isEmpty() ? "Unnamed" : combined;
    }

    private enum SearchScope {
        PLATFORM(List.of("Customer", "Organization", "Menu")),
        ORGANIZATION(List.of("Student", "Staff", "Class", "Lead")),
        TEACHER(List.of("Student")),
        NONE(List.of());

        private final List<String> categories;

        SearchScope(List<String> categories) {
            this.categories = categories;
        }

        List<String> categories() {
            return categories;
        }
    }
}
