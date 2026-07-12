package com.thinkerscave.staff.service.impl;

import com.thinkerscave.access.dto.UserCreationContext;
import com.thinkerscave.access.entity.Role;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.RoleRepository;
import com.thinkerscave.access.service.UserService;
import com.thinkerscave.document.entity.Document;
import com.thinkerscave.document.enums.DocumentOwnerType;
import com.thinkerscave.document.repository.DocumentRepository;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.dto.request.StaffCreateRequest;
import com.thinkerscave.staff.dto.request.StaffUpdateRequest;
import com.thinkerscave.staff.dto.response.*;
import com.thinkerscave.staff.entity.Payroll;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.EmploymentCategory;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.enums.StaffType;
import com.thinkerscave.staff.repository.PayrollRepository;
import com.thinkerscave.staff.repository.ResponsibilityAssignmentRepository;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.staff.repository.StaffSalaryStructureRepository;
import com.thinkerscave.staff.service.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final DocumentRepository documentRepository;
    private final StaffSalaryStructureRepository salaryStructureRepository;
    private final ResponsibilityAssignmentRepository assignmentRepository;
    private final PayrollRepository payrollRepository;

    @Override
    @Transactional
    public StaffCreateResponse createStaff(StaffCreateRequest request) {
        log.info("Creating staff with email: {}", request.getEmail());

        if (staffRepository.existsByEmail(request.getEmail())) {
            throw new AlreadyExistsException("Staff with email already exists: " + request.getEmail());
        }

        Role staffRole = roleRepository.findByRoleCode("ROLE_STAFF")
                .orElseGet(() -> roleRepository.findByRoleCode("STAFF")
                        .orElseGet(() -> roleRepository.findByRoleName("STAFF")
                                .orElse(null)));

        UserCreationContext context = new UserCreationContext(
                request.getFirstName(), request.getMiddleName(), request.getLastName(),
                request.getEmail(), request.getMobileNumber(),
                null, null, null, null
        );
        User user = userService.createUser(context, staffRole);

        Staff staff = new Staff();
        staff.setUser(user);
        mapCreateRequestToStaff(request, staff);
        staff.setStaffCode(generateStaffCode());
        staff.setActive(true);

        Staff saved = staffRepository.save(staff);
        log.info("Staff created with ID: {}, code: {}", saved.getStaffId(), saved.getStaffCode());

        return StaffCreateResponse.builder()
                .staffId(saved.getStaffId())
                .staffCode(saved.getStaffCode())
                .userId(user.getId())
                .build();
    }

    @Override
    @Transactional
    public void updateStaff(Long staffId, StaffUpdateRequest request) {
        Staff staff = getStaffEntity(staffId);
        mapUpdateRequestToStaff(request, staff);
        staffRepository.save(staff);
        log.info("Staff updated: {}", staffId);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffDetailResponse getStaffDetail(Long staffId) {
        Staff staff = getStaffEntity(staffId);
        return buildDetailResponse(staff);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StaffSummaryResponse> getStaffList(
            StaffType staffType, EmploymentCategory employmentCategory,
            EmploymentStatus employmentStatus, String designation,
            String keyword, Pageable pageable) {
        return staffRepository.searchStaff(staffType, employmentCategory, employmentStatus, designation, keyword, pageable)
                .map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffDashboardResponse getDashboard() {
        long total = staffRepository.count();
        long teaching = staffRepository.countByStaffType(StaffType.TEACHING);
        long nonTeaching = staffRepository.countByStaffType(StaffType.NON_TEACHING);
        long active = staffRepository.countActiveStaff();
        long temporary = staffRepository.countByEmploymentCategory(EmploymentCategory.TEMPORARY);
        long contract = staffRepository.countByEmploymentCategory(EmploymentCategory.CONTRACT);

        return StaffDashboardResponse.builder()
                .totalStaff(total)
                .teachingStaff(teaching)
                .nonTeachingStaff(nonTeaching)
                .activeStaff(active)
                .temporaryStaff(temporary)
                .contractStaff(contract)
                .build();
    }

    @Override
    @Transactional
    public void activateStaff(Long staffId) {
        Staff staff = getStaffEntity(staffId);
        staff.setActive(true);
        staff.setEmploymentStatus(EmploymentStatus.ACTIVE);
        staffRepository.save(staff);
    }

    @Override
    @Transactional
    public void deactivateStaff(Long staffId) {
        Staff staff = getStaffEntity(staffId);
        staff.setActive(false);
        staffRepository.save(staff);
    }

    @Override
    @Transactional
    public void deleteStaff(Long staffId) {
        Staff staff = getStaffEntity(staffId);
        staff.setActive(false);
        staffRepository.save(staff);
        log.info("Staff soft-deleted: {}", staffId);
    }

    // ---- Helpers ----

    Staff getStaffEntity(Long staffId) {
        return staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with ID: " + staffId));
    }

    private void mapCreateRequestToStaff(StaffCreateRequest req, Staff staff) {
        staff.setFirstName(req.getFirstName());
        staff.setMiddleName(req.getMiddleName());
        staff.setLastName(req.getLastName());
        staff.setGender(req.getGender());
        staff.setDateOfBirth(req.getDateOfBirth());
        staff.setBloodGroup(req.getBloodGroup());
        staff.setReligion(req.getReligion());
        staff.setNationality(req.getNationality());
        staff.setMobileNumber(req.getMobileNumber());
        staff.setEmail(req.getEmail());
        staff.setStaffType(req.getStaffType());
        staff.setDesignation(req.getDesignation());
        staff.setEmploymentCategory(req.getEmploymentCategory());
        staff.setEmploymentStatus(req.getEmploymentStatus());
        staff.setJoiningDate(req.getJoiningDate());
        staff.setHighestQualification(req.getHighestQualification());
        staff.setExperienceYears(req.getExperienceYears());
        staff.setEmergencyContactName(req.getEmergencyContactName());
        staff.setEmergencyContactRelation(req.getEmergencyContactRelation());
        staff.setEmergencyContactNumber(req.getEmergencyContactNumber());
        staff.setPhotoUrl(req.getPhotoUrl());
        staff.setRemarks(req.getRemarks());
    }

    private void mapUpdateRequestToStaff(StaffUpdateRequest req, Staff staff) {
        staff.setFirstName(req.getFirstName());
        staff.setMiddleName(req.getMiddleName());
        staff.setLastName(req.getLastName());
        staff.setGender(req.getGender());
        staff.setDateOfBirth(req.getDateOfBirth());
        staff.setBloodGroup(req.getBloodGroup());
        staff.setReligion(req.getReligion());
        staff.setNationality(req.getNationality());
        staff.setMobileNumber(req.getMobileNumber());
        staff.setStaffType(req.getStaffType());
        staff.setDesignation(req.getDesignation());
        staff.setEmploymentCategory(req.getEmploymentCategory());
        staff.setEmploymentStatus(req.getEmploymentStatus());
        staff.setJoiningDate(req.getJoiningDate());
        staff.setHighestQualification(req.getHighestQualification());
        staff.setExperienceYears(req.getExperienceYears());
        staff.setEmergencyContactName(req.getEmergencyContactName());
        staff.setEmergencyContactRelation(req.getEmergencyContactRelation());
        staff.setEmergencyContactNumber(req.getEmergencyContactNumber());
        staff.setPhotoUrl(req.getPhotoUrl());
        staff.setRemarks(req.getRemarks());
    }

    private String generateStaffCode() {
        long count = staffRepository.count() + 1;
        return "STF-" + String.format("%04d", count);
    }

    StaffDetailResponse buildDetailResponse(Staff staff) {
        List<Document> docs = documentRepository.findByOwnerTypeAndOwnerIdAndActiveTrue(
                DocumentOwnerType.STAFF, staff.getStaffId());

        List<ResponsibilityAssignmentResponse> responsibilities = assignmentRepository
                .findByStaff_StaffIdAndActiveTrueOrderByEffectiveFromDesc(staff.getStaffId())
                .stream()
                .map(a -> ResponsibilityAssignmentResponse.builder()
                        .assignmentId(a.getAssignmentId())
                        .staffId(staff.getStaffId())
                        .staffName(staff.getFirstName() + " " + staff.getLastName())
                        .staffCode(staff.getStaffCode())
                        .responsibilityId(a.getResponsibility().getResponsibilityId())
                        .responsibilityCode(a.getResponsibility().getResponsibilityCode())
                        .responsibilityName(a.getResponsibility().getResponsibilityName())
                        .scope(a.getScope())
                        .effectiveFrom(a.getEffectiveFrom())
                        .effectiveTo(a.getEffectiveTo())
                        .active(a.getActive())
                        .build())
                .collect(Collectors.toList());

        StaffDetailResponse.SalarySummary salarySummary = salaryStructureRepository
                .findByStaff_StaffIdAndActiveTrue(staff.getStaffId())
                .map(s -> StaffDetailResponse.SalarySummary.builder()
                        .salaryStructureId(s.getSalaryStructureId())
                        .salaryType(s.getSalaryType().name())
                        .grossSalary(s.getGrossSalary())
                        .effectiveFrom(s.getEffectiveFrom())
                        .build())
                .orElse(null);

        StaffDetailResponse.PayrollSummary payrollSummary = payrollRepository
                .findByStaff_StaffIdOrderByPayrollYearDescPayrollMonthDesc(staff.getStaffId())
                .stream()
                .findFirst()
                .map(p -> StaffDetailResponse.PayrollSummary.builder()
                        .lastPayrollMonth(p.getPayrollYear() + "-" + String.format("%02d", p.getPayrollMonth()))
                        .lastNetSalary(p.getNetSalary())
                        .lastPayrollStatus(p.getStatus().name())
                        .build())
                .orElse(null);

        List<DocumentResponse> docResponses = docs.stream()
                .map(d -> DocumentResponse.builder()
                        .documentId(d.getDocumentId())
                        .ownerType(d.getOwnerType().name())
                        .ownerId(d.getOwnerId())
                        .documentType(d.getDocumentType())
                        .documentName(d.getDocumentName())
                        .fileName(d.getFileName())
                        .filePath(d.getFilePath())
                        .fileExtension(d.getFileExtension())
                        .fileSize(d.getFileSize())
                        .mimeType(d.getMimeType())
                        .active(d.getActive())
                        .build())
                .collect(Collectors.toList());

        return StaffDetailResponse.builder()
                .staffId(staff.getStaffId())
                .staffCode(staff.getStaffCode())
                .userId(staff.getUser() != null ? staff.getUser().getId() : null)
                .firstName(staff.getFirstName())
                .middleName(staff.getMiddleName())
                .lastName(staff.getLastName())
                .fullName(staff.getFirstName() + " " + (staff.getMiddleName() != null ? staff.getMiddleName() + " " : "") + staff.getLastName())
                .gender(staff.getGender())
                .dateOfBirth(staff.getDateOfBirth())
                .bloodGroup(staff.getBloodGroup())
                .religion(staff.getReligion())
                .nationality(staff.getNationality())
                .mobileNumber(staff.getMobileNumber())
                .email(staff.getEmail())
                .photoUrl(staff.getPhotoUrl())
                .staffType(staff.getStaffType())
                .designation(staff.getDesignation())
                .employmentCategory(staff.getEmploymentCategory())
                .employmentStatus(staff.getEmploymentStatus())
                .joiningDate(staff.getJoiningDate())
                .highestQualification(staff.getHighestQualification())
                .experienceYears(staff.getExperienceYears())
                .emergencyContactName(staff.getEmergencyContactName())
                .emergencyContactRelation(staff.getEmergencyContactRelation())
                .emergencyContactNumber(staff.getEmergencyContactNumber())
                .active(staff.getActive())
                .remarks(staff.getRemarks())
                .createdOn(staff.getCreatedOn())
                .updatedOn(staff.getUpdatedOn())
                .salarySummary(salarySummary)
                .responsibilities(responsibilities)
                .payrollSummary(payrollSummary)
                .documents(docResponses)
                .build();
    }

    private StaffSummaryResponse toSummary(Staff staff) {
        return StaffSummaryResponse.builder()
                .staffId(staff.getStaffId())
                .staffCode(staff.getStaffCode())
                .fullName(staff.getFirstName() + " " + staff.getLastName())
                .email(staff.getEmail())
                .mobileNumber(staff.getMobileNumber())
                .photoUrl(staff.getPhotoUrl())
                .staffType(staff.getStaffType())
                .designation(staff.getDesignation())
                .employmentCategory(staff.getEmploymentCategory())
                .employmentStatus(staff.getEmploymentStatus())
                .joiningDate(staff.getJoiningDate())
                .active(staff.getActive())
                .build();
    }
}
