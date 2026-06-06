package com.thinkerscave.common.staff.workspace.service;

import com.thinkerscave.common.attendance.domain.Attendance;
import com.thinkerscave.common.attendance.repository.AttendanceRepository;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.leave.domain.LeaveRequest;
import com.thinkerscave.common.leave.domain.LeaveRequest.LeaveStatus;
import com.thinkerscave.common.leave.repository.LeaveRepository;
import com.thinkerscave.common.staff.domain.AlumniStaff;
import com.thinkerscave.common.staff.domain.Staff;
import com.thinkerscave.common.staff.domain.StaffDocument;
import com.thinkerscave.common.staff.domain.StaffResponsibility;
import com.thinkerscave.common.staff.domain.TeachingProfile;
import com.thinkerscave.common.staff.repository.AlumniStaffRepository;
import com.thinkerscave.common.staff.repository.StaffDocumentRepository;
import com.thinkerscave.common.staff.repository.StaffRepository;
import com.thinkerscave.common.staff.repository.StaffResponsibilityRepository;
import com.thinkerscave.common.staff.repository.TeachingProfileRepository;
import com.thinkerscave.common.staff.workspace.dto.StaffWorkspaceDtos.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffWorkspaceService {

    private final StaffRepository staffRepository;
    private final StaffDocumentRepository documentRepository;
    private final StaffResponsibilityRepository responsibilityRepository;
    private final TeachingProfileRepository teachingProfileRepository;
    private final AlumniStaffRepository alumniRepository;
    private final LeaveRepository leaveRepository;
    private final AttendanceRepository attendanceRepository;

    private static final int DEFAULT_LEAVE_ALLOWANCE = 24;

    // ------------------------------------------------------------------
    // KPI
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public StaffKpi kpi() {
        Long orgId = orgId();
        List<Staff> staffList = staffRepository.findByOrganizationId(orgId);
        LocalDate today = LocalDate.now();
        long onLeave = countOnLeaveToday(orgId, today);
        long newJoiners = staffList.stream()
                .filter(s -> s.getHireDate() != null && s.getHireDate().isAfter(today.minusDays(30)))
                .count();
        long teaching = staffList.stream().filter(this::isTeacher).count();
        return StaffKpi.builder()
                .totalEmployees(staffList.size())
                .teachingStaff(teaching)
                .nonTeachingStaff(staffList.size() - teaching)
                .onLeaveToday(onLeave)
                .newJoiners(newJoiners)
                .build();
    }

    // ------------------------------------------------------------------
    // Directory
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<StaffDirectoryCard> directorySearch(StaffSearchRequest req) {
        Long orgId = orgId();
        StaffSearchRequest filter = req == null ? new StaffSearchRequest() : req;
        LocalDate today = LocalDate.now();
        Map<Long, String> leaveTodayMap = leaveRepository.findByOrganizationIdOrderByCreatedDateDesc(orgId).stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                .filter(l -> l.getStartDate() != null && l.getEndDate() != null)
                .filter(l -> !today.isBefore(l.getStartDate()) && !today.isAfter(l.getEndDate()))
                .collect(Collectors.toMap(
                        LeaveRequest::getStaffId,
                        l -> Optional.ofNullable(l.getLeaveType()).map(Enum::name).orElse("ON_LEAVE"),
                        (a, b) -> a));

        return staffRepository.findByOrganizationId(orgId).stream()
                .filter(s -> Boolean.TRUE.equals(filter.getActiveOnly()) ? Boolean.TRUE.equals(s.getIsActive()) : true)
                .filter(s -> filter.getDepartmentId() == null
                        || (s.getDepartment() != null && Objects.equals(s.getDepartment().getId(), filter.getDepartmentId())))
                .filter(s -> filter.getBranchId() == null
                        || (s.getBranch() != null && Objects.equals(s.getBranch().getId(), filter.getBranchId())))
                .filter(s -> filter.getEmploymentType() == null
                        || filter.getEmploymentType().isBlank()
                        || filter.getEmploymentType().equalsIgnoreCase("ALL")
                        || (filter.getEmploymentType().equalsIgnoreCase("TEACHING") && isTeacher(s))
                        || (filter.getEmploymentType().equalsIgnoreCase("NON_TEACHING") && !isTeacher(s)))
                .filter(s -> matchesSearch(s, filter.getSearch()))
                .map(s -> toDirectoryCard(s, leaveTodayMap))
                .sorted(Comparator.comparing(StaffDirectoryCard::getFullName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Profile 360
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public StaffProfile360 profile360(Long staffId) {
        Long orgId = orgId();
        Staff s = staffRepository.findById(staffId)
                .filter(x -> Objects.equals(x.getOrganizationId(), orgId))
                .orElseThrow(() -> new EntityNotFoundException("Staff " + staffId + " not found"));

        Optional<TeachingProfile> tp = teachingProfileRepository.findByStaffIdAndOrganizationId(staffId, orgId);
        List<StaffResponsibility> resps = responsibilityRepository
                .findByOrganizationIdAndStaffIdOrderByCreatedDateDesc(orgId, staffId);

        StaffLeaveSnapshot leaveSnap = buildLeaveSnapshot(orgId, staffId);

        StaffOverview overview = StaffOverview.builder()
                .leaveBalance(leaveSnap.getBalance())
                .responsibilityCount(resps.size())
                .classesAssigned(0)
                .attendancePercent(computeAttendancePercent(orgId, staffId))
                .nextLeave(leaveSnap.getNextLeave())
                .yearsOfService(yearsOfService(s.getHireDate()))
                .keyResponsibilities(resps.stream().limit(5)
                        .map(r -> KeyResponsibility.builder()
                                .name(r.getResponsibilityName())
                                .scope(r.getScope())
                                .status(r.getStatus())
                                .build())
                        .collect(Collectors.toList()))
                .subjectLoad(List.of())
                .build();

        return StaffProfile360.builder()
                .staffId(s.getId())
                .staffCode(s.getStaffCode())
                .fullName(fullName(s))
                .designation(s.getDepartment() != null ? s.getDepartment().getDepartmentName() : null)
                .hireDate(s.getHireDate())
                .email(s.getEmail())
                .mobileNumber(s.getMobileNumber() != null ? String.valueOf(s.getMobileNumber()) : null)
                .photoUrl(s.getPhotoUrl())
                .overview(overview)
                .personal(buildPersonal(s))
                .employment(buildEmployment(s))
                .teaching(tp.map(this::toTeachingSnapshot).orElseGet(() -> StaffTeachingSnapshot.builder().build()))
                .leaveSnapshot(leaveSnap)
                .payroll(StaffPayrollSnapshot.builder().build())
                .build();
    }

    @Transactional(readOnly = true)
    public List<StaffTimelineEntry> timeline(Long staffId) {
        Long orgId = orgId();
        return leaveRepository.findByOrganizationIdAndStaffId(orgId, staffId).stream()
                .sorted(Comparator.comparing(LeaveRequest::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(l -> StaffTimelineEntry.builder()
                        .id(l.getId())
                        .date(l.getStartDate() != null ? l.getStartDate().toString() : null)
                        .title("Leave - " + l.getLeaveType())
                        .description("Status: " + l.getStatus() + (l.getReason() != null ? " — " + l.getReason() : ""))
                        .type("LEAVE")
                        .build())
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Responsibilities
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ResponsibilityKpi responsibilityKpi() {
        Long orgId = orgId();
        long total = responsibilityRepository.countByOrganizationId(orgId);
        long assignedToday = responsibilityRepository
                .findByOrganizationIdOrderByCreatedDateDesc(orgId).stream()
                .filter(r -> r.getEffectiveFrom() != null && r.getEffectiveFrom().equals(LocalDate.now()))
                .count();
        long unassigned = responsibilityRepository.countByOrganizationIdAndStaffIdIsNull(orgId);
        long custom = responsibilityRepository.findByOrganizationIdOrderByCreatedDateDesc(orgId).stream()
                .filter(r -> "CUSTOM".equalsIgnoreCase(r.getResponsibilityType()))
                .count();
        return ResponsibilityKpi.builder()
                .total(total).assignedToday(assignedToday).unassigned(unassigned).custom(custom).build();
    }

    @Transactional(readOnly = true)
    public List<ResponsibilityResponse> responsibilities() {
        Long orgId = orgId();
        Map<Long, String> staffNames = staffNames(orgId);
        return responsibilityRepository.findByOrganizationIdOrderByCreatedDateDesc(orgId).stream()
                .map(r -> toResponsibility(r, staffNames))
                .collect(Collectors.toList());
    }

    public ResponsibilityResponse addResponsibility(ResponsibilityRequest req) {
        Long orgId = orgId();
        StaffResponsibility r = StaffResponsibility.builder()
                .organizationId(orgId)
                .staffId(req.getStaffId())
                .responsibilityName(req.getResponsibilityName())
                .responsibilityType(req.getResponsibilityType())
                .scope(req.getScope())
                .effectiveFrom(req.getEffectiveFrom() != null ? req.getEffectiveFrom() : LocalDate.now())
                .effectiveTo(req.getEffectiveTo())
                .status(req.getStatus() != null ? req.getStatus() : (req.getStaffId() == null ? "UNASSIGNED" : "ASSIGNED"))
                .remarks(req.getRemarks())
                .build();
        StaffResponsibility saved = responsibilityRepository.save(r);
        return toResponsibility(saved, staffNames(orgId));
    }

    public ResponsibilityResponse updateResponsibility(Long responsibilityId, ResponsibilityRequest req) {
        Long orgId = orgId();
        StaffResponsibility r = responsibilityRepository
                .findByResponsibilityIdAndOrganizationId(responsibilityId, orgId)
                .orElseThrow(() -> new EntityNotFoundException("Responsibility " + responsibilityId + " not found"));
        r.setResponsibilityName(req.getResponsibilityName());
        r.setResponsibilityType(req.getResponsibilityType());
        r.setStaffId(req.getStaffId());
        r.setScope(req.getScope());
        r.setEffectiveFrom(req.getEffectiveFrom());
        r.setEffectiveTo(req.getEffectiveTo());
        if (req.getStatus() != null) r.setStatus(req.getStatus());
        r.setRemarks(req.getRemarks());
        return toResponsibility(responsibilityRepository.save(r), staffNames(orgId));
    }

    public void deleteResponsibility(Long responsibilityId) {
        Long orgId = orgId();
        StaffResponsibility r = responsibilityRepository
                .findByResponsibilityIdAndOrganizationId(responsibilityId, orgId)
                .orElseThrow(() -> new EntityNotFoundException("Responsibility " + responsibilityId + " not found"));
        responsibilityRepository.delete(r);
    }

    // ------------------------------------------------------------------
    // Leave & Availability
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public LeaveAvailabilityKpi leaveAvailabilityKpi() {
        Long orgId = orgId();
        LocalDate today = LocalDate.now();
        long onLeave = countOnLeaveToday(orgId, today);
        long upcoming = leaveRepository.findByOrganizationIdOrderByCreatedDateDesc(orgId).stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED && l.getStartDate() != null && l.getStartDate().isAfter(today))
                .count();
        long total = staffRepository.findByOrganizationId(orgId).size();
        long absent = attendanceRepository
                .findByOrganizationIdAndAttendanceDateAndAttendanceType(orgId, today, Attendance.AttendanceType.STAFF)
                .stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.ABSENT)
                .count();
        long present = Math.max(0, total - onLeave - absent);
        return LeaveAvailabilityKpi.builder()
                .presentToday(present)
                .onLeaveToday(onLeave)
                .absentToday(absent)
                .upcomingLeaves(upcoming)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TodayLeaveEntry> todayLeaves() {
        Long orgId = orgId();
        LocalDate today = LocalDate.now();
        return leaveRepository.findByOrganizationIdOrderByCreatedDateDesc(orgId).stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED || l.getStatus() == LeaveStatus.PENDING)
                .filter(l -> l.getStartDate() != null && l.getEndDate() != null)
                .filter(l -> !today.isBefore(l.getStartDate()) && !today.isAfter(l.getEndDate()))
                .map(l -> TodayLeaveEntry.builder()
                        .staffId(l.getStaffId())
                        .staffName(l.getStaffName())
                        .department(l.getDepartment())
                        .leaveType(l.getLeaveType() != null ? l.getLeaveType().name() : null)
                        .startDate(l.getStartDate())
                        .endDate(l.getEndDate())
                        .days(l.getDays())
                        .reason(l.getReason())
                        .status(l.getStatus().name())
                        .build())
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Documents Vault
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public StaffDocumentKpi documentKpi() {
        Long orgId = orgId();
        long total = documentRepository.countByOrganizationId(orgId);
        long verified = documentRepository.countByOrganizationIdAndStatus(orgId, "VERIFIED");
        long pending = documentRepository.countByOrganizationIdAndStatus(orgId, "PENDING");
        long missing = documentRepository.countByOrganizationIdAndStatus(orgId, "MISSING");
        long expired = documentRepository.countByOrganizationIdAndStatus(orgId, "EXPIRED");
        return StaffDocumentKpi.builder()
                .total(total).verified(verified).pending(pending).missing(missing).expired(expired)
                .build();
    }

    @Transactional(readOnly = true)
    public List<StaffDocumentEntry> documents(String category, Long staffId) {
        Long orgId = orgId();
        Map<Long, String> names = staffNames(orgId);
        List<StaffDocument> rows;
        if (staffId != null) {
            rows = documentRepository.findByStaffIdAndOrganizationIdOrderByCreatedDateDesc(staffId, orgId);
        } else if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            rows = documentRepository.findByOrganizationIdAndCategoryOrderByCreatedDateDesc(orgId, category);
        } else {
            rows = documentRepository.findByOrganizationIdOrderByCreatedDateDesc(orgId);
        }
        return rows.stream().map(d -> toDocumentEntry(d, names)).collect(Collectors.toList());
    }

    public StaffDocumentEntry addDocument(StaffDocumentRequest req) {
        Long orgId = orgId();
        StaffDocument d = StaffDocument.builder()
                .organizationId(orgId)
                .staffId(req.getStaffId())
                .category(req.getCategory())
                .documentType(req.getDocumentType())
                .fileName(req.getFileName())
                .fileUrl(req.getFileUrl())
                .fileSize(req.getFileSize())
                .status(req.getStatus() != null ? req.getStatus() : "PENDING")
                .expiresOn(req.getExpiresOn())
                .remarks(req.getRemarks())
                .build();
        return toDocumentEntry(documentRepository.save(d), staffNames(orgId));
    }

    public StaffDocumentEntry verifyDocument(Long documentId, String verifier) {
        Long orgId = orgId();
        StaffDocument d = documentRepository.findByDocumentIdAndOrganizationId(documentId, orgId)
                .orElseThrow(() -> new EntityNotFoundException("Document " + documentId + " not found"));
        d.setStatus("VERIFIED");
        d.setVerifiedBy(verifier);
        d.setVerifiedOn(LocalDate.now());
        return toDocumentEntry(documentRepository.save(d), staffNames(orgId));
    }

    public void deleteDocument(Long documentId) {
        Long orgId = orgId();
        StaffDocument d = documentRepository.findByDocumentIdAndOrganizationId(documentId, orgId)
                .orElseThrow(() -> new EntityNotFoundException("Document " + documentId + " not found"));
        documentRepository.delete(d);
    }

    // ------------------------------------------------------------------
    // Alumni Staff
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public AlumniStaffKpi alumniKpi() {
        Long orgId = orgId();
        long total = alumniRepository.countByOrganizationId(orgId);
        long retired = alumniRepository.countByOrganizationIdAndExitType(orgId, "RETIRED");
        long resigned = alumniRepository.countByOrganizationIdAndExitType(orgId, "RESIGNED");
        long contract = alumniRepository.countByOrganizationIdAndExitType(orgId, "CONTRACT_COMPLETED");
        return AlumniStaffKpi.builder()
                .total(total).retired(retired).resigned(resigned).contractCompleted(contract).build();
    }

    @Transactional(readOnly = true)
    public List<AlumniStaffResponse> alumniList() {
        Long orgId = orgId();
        return alumniRepository.findByOrganizationIdOrderByExitDateDesc(orgId).stream()
                .map(this::toAlumni)
                .collect(Collectors.toList());
    }

    public AlumniStaffResponse addAlumni(AlumniStaffRequest req) {
        Long orgId = orgId();
        Double yos = (req.getJoinedDate() != null && req.getExitDate() != null)
                ? ChronoUnit.DAYS.between(req.getJoinedDate(), req.getExitDate()) / 365.25
                : null;
        AlumniStaff a = AlumniStaff.builder()
                .organizationId(orgId)
                .staffId(req.getStaffId())
                .staffCode(req.getStaffCode())
                .fullName(req.getFullName())
                .lastDesignation(req.getLastDesignation())
                .department(req.getDepartment())
                .exitType(req.getExitType())
                .exitDate(req.getExitDate())
                .joinedDate(req.getJoinedDate())
                .yearsOfService(yos)
                .email(req.getEmail())
                .contact(req.getContact())
                .remarks(req.getRemarks())
                .build();
        return toAlumni(alumniRepository.save(a));
    }

    // ------------------------------------------------------------------
    // Teaching Profile
    // ------------------------------------------------------------------

    public StaffTeachingSnapshot saveTeachingProfile(TeachingProfileRequest req) {
        Long orgId = orgId();
        TeachingProfile tp = teachingProfileRepository.findByStaffIdAndOrganizationId(req.getStaffId(), orgId)
                .orElseGet(() -> TeachingProfile.builder()
                        .organizationId(orgId)
                        .staffId(req.getStaffId())
                        .build());
        tp.setSubjectsCanTeach(req.getSubjectsCanTeach());
        tp.setPreferredSubjects(req.getPreferredSubjects());
        tp.setTeachingLevels(req.getTeachingLevels());
        tp.setCanSubstituteFor(req.getCanSubstituteFor());
        tp.setCannotSubstituteFor(req.getCannotSubstituteFor());
        tp.setQualification(req.getQualification());
        tp.setExperienceYears(req.getExperienceYears());
        tp.setRemarks(req.getRemarks());
        return toTeachingSnapshot(teachingProfileRepository.save(tp));
    }

    // ==================================================================
    // helpers
    // ==================================================================

    private Long orgId() {
        Long id = OrganizationContext.getOrganizationId();
        return id != null ? id : 1L;
    }

    private boolean isTeacher(Staff s) {
        if (s.getDepartment() == null) return false;
        String name = s.getDepartment().getDepartmentName();
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.contains("math") || lower.contains("science") || lower.contains("english")
                || lower.contains("computer") || lower.contains("sport") || lower.contains("language")
                || lower.contains("humanit") || lower.contains("commerce") || lower.contains("teaching");
    }

    private boolean matchesSearch(Staff s, String search) {
        if (search == null || search.isBlank()) return true;
        String q = search.toLowerCase();
        return (s.getFirstName() != null && s.getFirstName().toLowerCase().contains(q))
                || (s.getLastName() != null && s.getLastName().toLowerCase().contains(q))
                || (s.getEmail() != null && s.getEmail().toLowerCase().contains(q))
                || (s.getStaffCode() != null && s.getStaffCode().toLowerCase().contains(q));
    }

    private StaffDirectoryCard toDirectoryCard(Staff s, Map<Long, String> leaveTodayMap) {
        boolean onLeave = leaveTodayMap.containsKey(s.getId());
        return StaffDirectoryCard.builder()
                .staffId(s.getId())
                .staffCode(s.getStaffCode())
                .firstName(s.getFirstName())
                .lastName(s.getLastName())
                .fullName(fullName(s))
                .email(s.getEmail())
                .mobileNumber(s.getMobileNumber() != null ? String.valueOf(s.getMobileNumber()) : null)
                .gender(s.getGender())
                .departmentName(s.getDepartment() != null ? s.getDepartment().getDepartmentName() : null)
                .branchName(s.getBranch() != null ? s.getBranch().getBranchName() : null)
                .designation(s.getRemarks())
                .hireDate(s.getHireDate())
                .photoUrl(s.getPhotoUrl())
                .isActive(s.getIsActive())
                .availabilityStatus(onLeave ? "ON_LEAVE" : (Boolean.TRUE.equals(s.getIsActive()) ? "PRESENT" : "ABSENT"))
                .build();
    }

    private long countOnLeaveToday(Long orgId, LocalDate today) {
        return leaveRepository.findByOrganizationIdOrderByCreatedDateDesc(orgId).stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                .filter(l -> l.getStartDate() != null && l.getEndDate() != null)
                .filter(l -> !today.isBefore(l.getStartDate()) && !today.isAfter(l.getEndDate()))
                .count();
    }

    private StaffPersonal buildPersonal(Staff s) {
        return StaffPersonal.builder()
                .firstName(s.getFirstName())
                .middleName(s.getMiddleName())
                .lastName(s.getLastName())
                .gender(s.getGender())
                .dateOfBirth(s.getDateOfBirth())
                .email(s.getEmail())
                .mobileNumber(s.getMobileNumber() != null ? String.valueOf(s.getMobileNumber()) : null)
                .address(s.getAddress())
                .city(s.getCity())
                .state(s.getState())
                .build();
    }

    private StaffEmployment buildEmployment(Staff s) {
        return StaffEmployment.builder()
                .staffCode(s.getStaffCode())
                .departmentName(s.getDepartment() != null ? s.getDepartment().getDepartmentName() : null)
                .branchName(s.getBranch() != null ? s.getBranch().getBranchName() : null)
                .hireDate(s.getHireDate())
                .yearsOfService(yearsOfService(s.getHireDate()))
                .employmentType(isTeacher(s) ? "TEACHING" : "NON_TEACHING")
                .designation(s.getRemarks())
                .isActive(s.getIsActive())
                .build();
    }

    private StaffTeachingSnapshot toTeachingSnapshot(TeachingProfile tp) {
        return StaffTeachingSnapshot.builder()
                .subjectsCanTeach(tp.getSubjectsCanTeach())
                .preferredSubjects(tp.getPreferredSubjects())
                .teachingLevels(tp.getTeachingLevels())
                .canSubstituteFor(tp.getCanSubstituteFor())
                .cannotSubstituteFor(tp.getCannotSubstituteFor())
                .qualification(tp.getQualification())
                .experienceYears(tp.getExperienceYears())
                .build();
    }

    private StaffLeaveSnapshot buildLeaveSnapshot(Long orgId, Long staffId) {
        List<LeaveRequest> mine = leaveRepository.findByOrganizationIdAndStaffId(orgId, staffId);
        int used = mine.stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                .mapToInt(l -> Optional.ofNullable(l.getDays()).orElse(0))
                .sum();
        String next = mine.stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED && l.getStartDate() != null && l.getStartDate().isAfter(LocalDate.now()))
                .sorted(Comparator.comparing(LeaveRequest::getStartDate))
                .findFirst()
                .map(l -> l.getStartDate().toString() + " — " + l.getLeaveType())
                .orElse(null);
        return StaffLeaveSnapshot.builder()
                .totalAllowance(DEFAULT_LEAVE_ALLOWANCE)
                .used(used)
                .balance(Math.max(0, DEFAULT_LEAVE_ALLOWANCE - used))
                .nextLeave(next)
                .build();
    }

    private Double computeAttendancePercent(Long orgId, Long staffId) {
        List<Attendance> rows = attendanceRepository
                .findByOrganizationIdAndReferenceIdAndAttendanceType(orgId, staffId, Attendance.AttendanceType.STAFF);
        if (rows.isEmpty()) return 95.0;
        long total = rows.size();
        long present = rows.stream()
                .filter(r -> r.getStatus() == Attendance.AttendanceStatus.PRESENT
                        || r.getStatus() == Attendance.AttendanceStatus.LATE
                        || r.getStatus() == Attendance.AttendanceStatus.HALF_DAY)
                .count();
        return Math.round((present * 1000.0 / total)) / 10.0;
    }

    private Double yearsOfService(LocalDate hireDate) {
        if (hireDate == null) return null;
        return Math.round(ChronoUnit.DAYS.between(hireDate, LocalDate.now()) / 365.25 * 10.0) / 10.0;
    }

    private String fullName(Staff s) {
        StringBuilder sb = new StringBuilder();
        if (s.getFirstName() != null) sb.append(s.getFirstName());
        if (s.getMiddleName() != null && !s.getMiddleName().isBlank()) sb.append(' ').append(s.getMiddleName());
        if (s.getLastName() != null) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(s.getLastName());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private Map<Long, String> staffNames(Long orgId) {
        return staffRepository.findByOrganizationId(orgId).stream()
                .collect(Collectors.toMap(Staff::getId, this::fullName, (a, b) -> a));
    }

    private ResponsibilityResponse toResponsibility(StaffResponsibility r, Map<Long, String> staffNames) {
        return ResponsibilityResponse.builder()
                .responsibilityId(r.getResponsibilityId())
                .responsibilityName(r.getResponsibilityName())
                .responsibilityType(r.getResponsibilityType())
                .staffId(r.getStaffId())
                .staffName(r.getStaffId() != null ? staffNames.get(r.getStaffId()) : null)
                .scope(r.getScope())
                .effectiveFrom(r.getEffectiveFrom())
                .effectiveTo(r.getEffectiveTo())
                .status(r.getStatus())
                .remarks(r.getRemarks())
                .build();
    }

    private StaffDocumentEntry toDocumentEntry(StaffDocument d, Map<Long, String> staffNames) {
        return StaffDocumentEntry.builder()
                .documentId(d.getDocumentId())
                .staffId(d.getStaffId())
                .staffName(staffNames.get(d.getStaffId()))
                .category(d.getCategory())
                .documentType(d.getDocumentType())
                .fileName(d.getFileName())
                .fileUrl(d.getFileUrl())
                .fileSize(d.getFileSize())
                .status(d.getStatus())
                .verifiedBy(d.getVerifiedBy())
                .verifiedOn(d.getVerifiedOn())
                .expiresOn(d.getExpiresOn())
                .remarks(d.getRemarks())
                .uploadedOn(d.getCreatedDate() != null ? d.getCreatedDate().toString() : null)
                .build();
    }

    private AlumniStaffResponse toAlumni(AlumniStaff a) {
        return AlumniStaffResponse.builder()
                .alumniStaffId(a.getAlumniStaffId())
                .staffId(a.getStaffId())
                .staffCode(a.getStaffCode())
                .fullName(a.getFullName())
                .lastDesignation(a.getLastDesignation())
                .department(a.getDepartment())
                .exitType(a.getExitType())
                .exitDate(a.getExitDate())
                .joinedDate(a.getJoinedDate())
                .yearsOfService(a.getYearsOfService())
                .email(a.getEmail())
                .contact(a.getContact())
                .remarks(a.getRemarks())
                .build();
    }
}
