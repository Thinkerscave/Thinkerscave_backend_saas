package com.thinkerscave.dashboard.service;

import com.thinkerscave.admission.entity.Inquiry;
import com.thinkerscave.admission.repository.InquiryRepository;
import com.thinkerscave.dashboard.dto.DashboardSearchResponseDTO;
import com.thinkerscave.dashboard.dto.DashboardSearchResultDTO;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardSearchService {

    private static final int LIMIT = 8;

    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final InquiryRepository inquiryRepository;

    public DashboardSearchResponseDTO search(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.length() < 2) {
            return DashboardSearchResponseDTO.builder()
                    .query(normalized)
                    .results(List.of())
                    .supportedCategories(List.of("Student", "Staff", "Lead"))
                    .build();
        }

        Long orgId = OrganizationContext.getOrganizationId();
        List<DashboardSearchResultDTO> results = new ArrayList<>();
        results.addAll(searchStudents(normalized));
        results.addAll(searchStaff(normalized));
        if (orgId != null) {
            results.addAll(searchLeads(orgId, normalized));
        }

        return DashboardSearchResponseDTO.builder()
                .query(normalized)
                .results(results.stream().limit(24).toList())
                .supportedCategories(List.of("Student", "Staff", "Lead"))
                .build();
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

    private List<DashboardSearchResultDTO> searchLeads(Long orgId, String keyword) {
        return inquiryRepository.search(keyword, PageRequest.of(0, LIMIT))
                .stream()
                .map(this::toLeadResult)
                .toList();
    }

    private DashboardSearchResultDTO toStudentResult(Student student) {
        String title = joinName(student.getFirstName(), student.getLastName());
        return DashboardSearchResultDTO.builder()
                .key("student-" + student.getStudentId())
                .entityType("Student")
                .entityId(String.valueOf(student.getStudentId()))
                .title(title)
                .subtitle(student.getStudentCode())
                .detail(student.getAdmissionNumber())
                .icon("pi pi-user")
                .route("/app/students/profile/" + student.getStudentId())
                .tone("primary")
                .build();
    }

    private DashboardSearchResultDTO toStaffResult(Staff staff) {
        String title = joinName(staff.getFirstName(), staff.getLastName());
        return DashboardSearchResultDTO.builder()
                .key("staff-" + staff.getStaffId())
                .entityType("Staff")
                .entityId(String.valueOf(staff.getStaffId()))
                .title(title)
                .subtitle(staff.getStaffCode())
                .detail(staff.getDesignation())
                .icon("pi pi-briefcase")
                .route("/app/staff/profile/" + staff.getStaffId())
                .tone("info")
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

    private String joinName(String first, String last) {
        String combined = String.join(" ",
                StringUtils.hasText(first) ? first.trim() : "",
                StringUtils.hasText(last) ? last.trim() : "").trim();
        return combined.isEmpty() ? "Unnamed" : combined;
    }
}
