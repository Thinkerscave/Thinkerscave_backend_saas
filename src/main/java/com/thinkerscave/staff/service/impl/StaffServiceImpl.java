package com.thinkerscave.staff.service.impl;

import com.thinkerscave.access.dto.UserCreationContext;
import com.thinkerscave.access.entity.Role;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.RoleRepository;
import com.thinkerscave.access.service.UserService;
import com.thinkerscave.document.entity.Document;
import com.thinkerscave.document.enums.DocumentOwnerType;
import com.thinkerscave.document.repository.DocumentRepository;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.dto.AddressDto;
import com.thinkerscave.shared.entity.Address;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.shared.service.AddressFactory;
import com.thinkerscave.staff.dto.request.StaffCreateRequest;
import com.thinkerscave.staff.dto.request.StaffUpdateRequest;
import com.thinkerscave.staff.dto.response.*;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.EmploymentCategory;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.enums.StaffType;
import com.thinkerscave.staff.repository.ResponsibilityAssignmentRepository;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.staff.security.StaffBankAccountCipher;
import com.thinkerscave.staff.service.StaffBankDetailsService;
import com.thinkerscave.staff.service.StaffCodeGenerator;
import com.thinkerscave.staff.enums.StaffActivityType;
import com.thinkerscave.staff.service.StaffActivityService;
import com.thinkerscave.staff.service.StaffDocumentService;
import com.thinkerscave.staff.service.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final DocumentRepository documentRepository;
    private final ResponsibilityAssignmentRepository assignmentRepository;
    private final StaffCodeGenerator staffCodeGenerator;
    private final StaffBankDetailsService bankDetailsService;
    private final StaffBankAccountCipher bankCipher;
    private final AddressFactory addressFactory;
    private final StaffActivityService activityService;

    private static final String SECTION_PERSONAL = "Personal details";
    private static final String SECTION_PROFESSIONAL = "Professional details";
    private static final String SECTION_EMERGENCY = "Emergency contact";
    private static final String SECTION_ADDRESS = "Address";
    private static final String SECTION_REMARKS = "Remarks";

    @Override
    @Transactional
    public StaffCreateResponse createStaff(StaffCreateRequest request) {
        log.info("Creating staff with email: {}", request.getEmail());

        Long organizationId = OrganizationContext.getOrganizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required to create staff");
        }
        if (request.getBankDetails() != null && !bankCipher.isConfigured()) {
            // Fail before any user/staff row is written: bank numbers must never be stored unencrypted.
            throw new IllegalStateException(
                    "STAFF_BANK_ENC_KEY is not configured; bank details cannot be saved");
        }

        if (staffRepository.existsByEmail(request.getEmail())) {
            throw new AlreadyExistsException("Staff with email already exists: " + request.getEmail());
        }
        validateContractDates(request.getContractStartDate(), request.getContractEndDate());

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
        staff.setStaffCode(staffCodeGenerator.nextStaffCode(organizationId));
        staff.setActive(true);
        applyAddresses(request, staff);

        Staff saved = staffRepository.save(staff);
        if (request.getBankDetails() != null) {
            bankDetailsService.apply(saved, request.getBankDetails());
        }
        activityService.record(saved, StaffActivityType.CREATED, null, "Staff profile created");
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
        Map<String, List<Object>> before = profileSections(staff);
        mapUpdateRequestToStaff(request, staff);
        applyAddressesOnUpdate(request, staff);
        Staff saved = staffRepository.save(staff);
        profileSections(saved).forEach((section, value) -> {
            if (!value.equals(before.get(section))) {
                activityService.record(saved, StaffActivityType.PROFILE_UPDATED, section,
                        "Updated " + section.toLowerCase());
            }
        });
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
        // Same basis as the staff list: soft-deleted/deactivated rows are excluded from every KPI.
        long total = staffRepository.countByActive(true);
        long teaching = staffRepository.countByActiveTrueAndStaffType(StaffType.TEACHING);
        long nonTeaching = staffRepository.countByActiveTrueAndStaffType(StaffType.NON_TEACHING);
        long active = staffRepository.countByActiveTrueAndEmploymentStatus(EmploymentStatus.ACTIVE);
        long temporary = staffRepository.countByActiveTrueAndEmploymentCategory(EmploymentCategory.TEMPORARY);
        long contract = staffRepository.countByActiveTrueAndEmploymentCategory(EmploymentCategory.CONTRACT);

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
        activityService.record(staff, StaffActivityType.STATUS_CHANGED, "Status", "Activated staff member");
    }

    @Override
    @Transactional
    public void deactivateStaff(Long staffId) {
        Staff staff = getStaffEntity(staffId);
        staff.setActive(false);
        staffRepository.save(staff);
        activityService.record(staff, StaffActivityType.STATUS_CHANGED, "Status", "Deactivated staff member");
    }

    @Override
    @Transactional
    public void deleteStaff(Long staffId) {
        Staff staff = getStaffEntity(staffId);
        staff.setActive(false);
        staffRepository.save(staff);
        activityService.record(staff, StaffActivityType.STATUS_CHANGED, "Status", "Removed staff member (deactivated)");
        log.info("Staff soft-deleted: {}", staffId);
    }

    // ---- Helpers ----

    /** Snapshot of each profile section's values; used to detect which sections changed on update. */
    private Map<String, List<Object>> profileSections(Staff s) {
        Map<String, List<Object>> sections = new LinkedHashMap<>();
        sections.put(SECTION_PERSONAL, Arrays.<Object>asList(s.getFirstName(), s.getMiddleName(), s.getLastName(),
                s.getGender(), s.getDateOfBirth(), s.getBloodGroup(), s.getReligion(), s.getNationality(),
                s.getMobileNumber()));
        sections.put(SECTION_PROFESSIONAL, Arrays.<Object>asList(s.getStaffType(), s.getDesignation(),
                s.getEmploymentCategory(), s.getEmploymentStatus(), s.getJoiningDate(), s.getHighestQualification(),
                s.getExperienceYears(), s.getContractStartDate(), s.getContractEndDate()));
        sections.put(SECTION_EMERGENCY, Arrays.<Object>asList(s.getEmergencyContactName(),
                s.getEmergencyContactRelation(), s.getEmergencyContactNumber()));
        sections.put(SECTION_ADDRESS, Arrays.<Object>asList(addressKey(s.getPresentAddress()),
                addressKey(s.getPermanentAddress()), Boolean.TRUE.equals(s.getSameAsPresentAddress())));
        sections.put(SECTION_REMARKS, Arrays.<Object>asList(s.getRemarks()));
        return sections;
    }

    private static String addressKey(Address a) {
        if (a == null || Boolean.FALSE.equals(a.getActive())) {
            return "";
        }
        return Stream.of(a.getAddressLine1(), a.getAddressLine2(), a.getLandmark(), a.getCity(), a.getDistrict(),
                        a.getState(), a.getCountry(), a.getPostalCode())
                .map(v -> v == null ? "" : v.trim())
                .collect(Collectors.joining("|"));
    }

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
        staff.setContractStartDate(req.getContractStartDate());
        staff.setContractEndDate(req.getContractEndDate());
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
        staff.setContractStartDate(req.getContractStartDate());
        staff.setContractEndDate(req.getContractEndDate());
        staff.setRemarks(req.getRemarks());
    }

    /** Contract end date must not be before the start date (both optional). */
    private void validateContractDates(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BadRequestException("Contract end date cannot be before the start date");
        }
    }

    private void applyAddressesOnUpdate(StaffUpdateRequest req, Staff staff) {
        validateContractDates(req.getContractStartDate(), req.getContractEndDate());
        Address present = addressFactory.build(req.getPresentAddress());
        boolean same = Boolean.TRUE.equals(req.getSameAsPresentAddress()) && present != null;
        staff.setPresentAddress(replaceAddress(staff.getPresentAddress(), present));
        staff.setSameAsPresentAddress(same);
        Address permanent = same ? addressFactory.copy(present) : addressFactory.build(req.getPermanentAddress());
        staff.setPermanentAddress(replaceAddress(staff.getPermanentAddress(), permanent));
    }

    /** Updates the existing address row in place; removes it when the form cleared the address. */
    private Address replaceAddress(Address existing, Address fresh) {
        if (fresh == null) {
            if (existing != null) {
                existing.setActive(false);
            }
            return null;
        }
        if (existing == null) {
            return fresh;
        }
        existing.setAddressLine1(fresh.getAddressLine1());
        existing.setAddressLine2(fresh.getAddressLine2());
        existing.setLandmark(fresh.getLandmark());
        existing.setCity(fresh.getCity());
        existing.setDistrict(fresh.getDistrict());
        existing.setState(fresh.getState());
        existing.setCountry(fresh.getCountry());
        existing.setPostalCode(fresh.getPostalCode());
        existing.setActive(true);
        return existing;
    }

    private void applyAddresses(StaffCreateRequest req, Staff staff) {
        Address present = addressFactory.build(req.getPresentAddress());
        boolean same = Boolean.TRUE.equals(req.getSameAsPresentAddress()) && present != null;
        staff.setPresentAddress(present);
        staff.setSameAsPresentAddress(same);
        if (same) {
            staff.setPermanentAddress(addressFactory.copy(present));
        } else {
            staff.setPermanentAddress(addressFactory.build(req.getPermanentAddress()));
        }
    }

    StaffDetailResponse buildDetailResponse(Staff staff) {
        List<Document> docs;
        try {
            docs = documentRepository.findByOwnerTypeAndOwnerIdAndActiveTrue(
                    DocumentOwnerType.STAFF, staff.getStaffId()).stream()
                    .filter(d -> !StaffDocumentService.PHOTO_TYPE.equals(d.getDocumentType()))
                    .toList();
        } catch (RuntimeException ex) {
            log.warn("Staff documents unavailable for {}: {}", staff.getStaffId(), ex.getMessage());
            docs = List.of();
        }

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

        // Salary/payroll summaries live under Finance Payroll — omit legacy Staff payroll reads.
        StaffDetailResponse.SalarySummary salarySummary = null;
        StaffDetailResponse.PayrollSummary payrollSummary = null;

        List<DocumentResponse> docResponses = docs.stream()
                .map(d -> DocumentResponse.builder()
                        .documentId(d.getDocumentId())
                        .ownerType(d.getOwnerType().name())
                        .ownerId(d.getOwnerId())
                        .documentType(d.getDocumentType())
                        .documentName(d.getDocumentName())
                        .fileName(d.getFileName())
                        .filePath(null)
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
                .contractStartDate(staff.getContractStartDate())
                .contractEndDate(staff.getContractEndDate())
                .active(staff.getActive())
                .remarks(staff.getRemarks())
                .createdOn(staff.getCreatedOn())
                .updatedOn(staff.getUpdatedOn())
                .salarySummary(salarySummary)
                .responsibilities(responsibilities)
                .payrollSummary(payrollSummary)
                .documents(docResponses)
                .presentAddress(addressFactory.toDto(staff.getPresentAddress()))
                .permanentAddress(addressFactory.toDto(staff.getPermanentAddress()))
                .sameAsPresentAddress(Boolean.TRUE.equals(staff.getSameAsPresentAddress()))
                .bankDetails(bankDetailsService.findMasked(staff.getStaffId()).orElse(null))
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
