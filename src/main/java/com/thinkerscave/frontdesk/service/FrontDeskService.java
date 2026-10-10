package com.thinkerscave.frontdesk.service;

import com.thinkerscave.admission.enums.LeadSource;
import com.thinkerscave.admission.repository.InquiryFollowUpRepository;
import com.thinkerscave.admission.repository.InquiryRepository;
import com.thinkerscave.attendance.entity.StudentAttendance;
import com.thinkerscave.attendance.repository.StudentAttendanceRepository;
import com.thinkerscave.frontdesk.entity.*;
import com.thinkerscave.frontdesk.repository.*;
import com.thinkerscave.frontdesk.security.FrontDeskAccessGuard;
import com.thinkerscave.security.service.SmsService;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.shared.storage.LocalFileStorageService;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.student.entity.Parent;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.entity.StudentParent;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentParentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import com.thinkerscave.student.repository.TransferRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class FrontDeskService {

    private static final int DEFAULT_OVERSTAY_HOURS = 8;

    private final FrontDeskAccessGuard guard;
    private final FrontDeskVisitorRepository visitors;
    private final FrontDeskAppointmentRepository appointments;
    private final FrontDeskPostalInwardRepository inwardMail;
    private final FrontDeskPostalOutwardRepository outwardMail;
    private final FrontDeskCallLogRepository calls;
    private final FrontDeskGrievanceRepository grievances;
    private final FrontDeskCertificateRequestRepository certificates;
    private final FrontDeskGatePassRepository gatePasses;
    private final FrontDeskGateOtpRepository otps;
    private final FrontDeskSettingRepository settings;
    private final StudentAttendanceRepository attendance;
    private final StudentEnrollmentRepository enrollments;
    private final StudentRepository students;
    private final StudentParentRepository studentParents;
    private final StaffRepository staff;
    private final TransferRequestRepository transfers;
    private final InquiryRepository inquiries;
    private final InquiryFollowUpRepository followUps;
    private final LocalFileStorageService files;
    private final SmsService sms;
    private final SecureRandom random = new SecureRandom();

    @Transactional(readOnly = true)
    public Map<String, Long> summary() {
        guard.requireView(FrontDeskAccessGuard.FRONT_DESK);
        Long orgId = guard.organizationId();
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        String user = guard.username();
        long pendingFollowUps = 0;
        if (user != null) {
            pendingFollowUps = followUps.findTodayForDesk(user, start, end).size()
                    + followUps.findOverdueForDesk(user, start).size();
        }
        long walkIns = inquiries.countByInquirySourceAndCreatedOnBetweenAndDeletedFalse(
                LeadSource.WALK_IN, start, end);
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("visitorsOnCampus", visitors.countByOrganizationIdAndOutTimeIsNull(orgId));
        counts.put("todaysAppointments", appointments.countByOrganizationIdAndRequestedSlotBetween(orgId, start, end));
        counts.put("pendingFollowUps", pendingFollowUps);
        counts.put("todaysDispatches", outwardMail.countByOrganizationIdAndDispatchDate(orgId, today));
        counts.put("todaysWalkIns", walkIns);
        return counts;
    }

    @Transactional(readOnly = true)
    public Page<FrontDeskVisitor> searchVisitors(String keyword, boolean activeOnly, Pageable pageable) {
        guard.requireView(FrontDeskAccessGuard.VISITORS);
        String q = blankToNull(keyword);
        Long orgId = guard.organizationId();
        Page<FrontDeskVisitor> page = activeOnly
                ? visitors.searchActive(orgId, q, pageable)
                : visitors.search(orgId, q, pageable);
        int hours = overstayHours(orgId);
        page.forEach(row -> markOverstay(row, hours));
        return page;
    }

    public FrontDeskVisitor createVisitor(FrontDeskVisitor body) {
        guard.requireManage(FrontDeskAccessGuard.VISITORS);
        body.setId(null);
        body.setOrganizationId(guard.organizationId());
        if (!StringUtils.hasText(body.getVisitorName()) || !StringUtils.hasText(body.getMobileNumber())) {
            throw new BadRequestException("Visitor name and mobile are required");
        }
        if (!StringUtils.hasText(body.getPurpose())) {
            throw new BadRequestException("Purpose is required");
        }
        if (body.getInTime() == null) {
            body.setInTime(LocalDateTime.now());
        }
        body.setOutTime(null);
        FrontDeskVisitor saved = visitors.save(body);
        markOverstay(saved, overstayHours(saved.getOrganizationId()));
        return saved;
    }

    public FrontDeskVisitor updateVisitor(Long id, FrontDeskVisitor body) {
        guard.requireManage(FrontDeskAccessGuard.VISITORS);
        FrontDeskVisitor existing = visitor(id);
        LocalDateTime originalIn = existing.getInTime();
        existing.setVisitorName(body.getVisitorName());
        existing.setMobileNumber(body.getMobileNumber());
        existing.setIdType(body.getIdType());
        existing.setIdNumber(body.getIdNumber());
        existing.setPurpose(body.getPurpose());
        existing.setMeetPersonType(body.getMeetPersonType());
        existing.setMeetPersonId(body.getMeetPersonId());
        existing.setMeetPersonName(body.getMeetPersonName());
        existing.setVehicleNumber(body.getVehicleNumber());
        existing.setBadgeNumber(body.getBadgeNumber());
        existing.setPhotoPath(body.getPhotoPath() != null ? body.getPhotoPath() : existing.getPhotoPath());
        existing.setInTime(originalIn);
        FrontDeskVisitor saved = visitors.save(existing);
        markOverstay(saved, overstayHours(saved.getOrganizationId()));
        return saved;
    }

    public FrontDeskVisitor checkOut(Long id) {
        guard.requireManage(FrontDeskAccessGuard.VISITORS);
        FrontDeskVisitor existing = visitor(id);
        if (existing.getOutTime() != null) {
            throw new BadRequestException("This visit is already closed");
        }
        existing.setOutTime(LocalDateTime.now());
        return visitors.save(existing);
    }

    public FrontDeskVisitor storePhoto(Long id, MultipartFile file) throws java.io.IOException {
        guard.requireManage(FrontDeskAccessGuard.VISITORS);
        FrontDeskVisitor existing = visitor(id);
        existing.setPhotoPath(files.store(file, "visitors"));
        return visitors.save(existing);
    }

    @Transactional(readOnly = true)
    public Page<FrontDeskAppointment> searchAppointments(String keyword, Pageable pageable) {
        guard.requireView(FrontDeskAccessGuard.APPOINTMENTS);
        return appointments.search(guard.organizationId(), blankToNull(keyword), pageable);
    }

    public FrontDeskAppointment saveAppointment(Long id, FrontDeskAppointment body) {
        guard.requireManage(FrontDeskAccessGuard.APPOINTMENTS);
        FrontDeskAppointment row = id == null ? new FrontDeskAppointment() : appointment(id);
        row.setOrganizationId(guard.organizationId());
        row.setVisitorName(body.getVisitorName());
        row.setStaffId(body.getStaffId());
        row.setStaffName(resolveStaffName(body.getStaffId(), body.getStaffName()));
        row.setRequestedSlot(body.getRequestedSlot());
        row.setAgenda(body.getAgenda());
        row.setStatus(StringUtils.hasText(body.getStatus()) ? body.getStatus() : "PENDING");
        return appointments.save(row);
    }

    public FrontDeskAppointment setAppointmentStatus(Long id, String status, LocalDateTime slot) {
        guard.requireManage(FrontDeskAccessGuard.APPOINTMENTS);
        FrontDeskAppointment row = appointment(id);
        row.setStatus(status);
        if (slot != null) {
            row.setRequestedSlot(slot);
        }
        return appointments.save(row);
    }

    @Transactional(readOnly = true)
    public Page<FrontDeskPostalInward> searchInward(String keyword, Pageable pageable) {
        guard.requireView(FrontDeskAccessGuard.POSTAL);
        return inwardMail.search(guard.organizationId(), blankToNull(keyword), pageable);
    }

    public FrontDeskPostalInward saveInward(Long id, FrontDeskPostalInward body) {
        guard.requireManage(FrontDeskAccessGuard.POSTAL);
        FrontDeskPostalInward row = id == null ? new FrontDeskPostalInward() : inward(id);
        row.setOrganizationId(guard.organizationId());
        row.setSenderName(body.getSenderName());
        row.setTrackingNumber(body.getTrackingNumber());
        row.setCourierPartner(body.getCourierPartner());
        row.setAddressedTo(body.getAddressedTo());
        row.setReceivedAt(body.getReceivedAt() != null ? body.getReceivedAt() : LocalDateTime.now());
        row.setStatus(StringUtils.hasText(body.getStatus()) ? body.getStatus() : "RECEIVED");
        return inwardMail.save(row);
    }

    public FrontDeskPostalInward handOver(Long id) {
        guard.requireManage(FrontDeskAccessGuard.POSTAL);
        FrontDeskPostalInward row = inward(id);
        row.setStatus("HANDED_OVER");
        return inwardMail.save(row);
    }

    @Transactional(readOnly = true)
    public Page<FrontDeskPostalOutward> searchOutward(String keyword, Pageable pageable) {
        guard.requireView(FrontDeskAccessGuard.POSTAL);
        return outwardMail.search(guard.organizationId(), blankToNull(keyword), pageable);
    }

    public FrontDeskPostalOutward saveOutward(Long id, FrontDeskPostalOutward body) {
        guard.requireManage(FrontDeskAccessGuard.POSTAL);
        FrontDeskPostalOutward row = id == null ? new FrontDeskPostalOutward() : outward(id);
        row.setOrganizationId(guard.organizationId());
        row.setRecipientName(body.getRecipientName());
        row.setDestinationAddress(body.getDestinationAddress());
        row.setDocumentType(body.getDocumentType());
        row.setDispatchMode(body.getDispatchMode());
        row.setDispatchDate(body.getDispatchDate() != null ? body.getDispatchDate() : LocalDate.now());
        row.setTrackingNumber(body.getTrackingNumber());
        row.setSenderReference(body.getSenderReference());
        return outwardMail.save(row);
    }

    @Transactional(readOnly = true)
    public Page<FrontDeskCallLog> searchCalls(String keyword, Pageable pageable) {
        guard.requireView(FrontDeskAccessGuard.CALLS);
        return calls.search(guard.organizationId(), blankToNull(keyword), pageable);
    }

    public FrontDeskCallLog saveCall(Long id, FrontDeskCallLog body) {
        guard.requireManage(FrontDeskAccessGuard.CALLS);
        FrontDeskCallLog row = id == null ? new FrontDeskCallLog() : call(id);
        row.setOrganizationId(guard.organizationId());
        row.setCallerName(body.getCallerName());
        row.setPhoneNumber(body.getPhoneNumber());
        row.setCallType(body.getCallType());
        row.setDurationMinutes(body.getDurationMinutes());
        row.setPurpose(body.getPurpose());
        row.setActionTaken(body.getActionTaken());
        row.setForwardedTo(body.getForwardedTo());
        return calls.save(row);
    }

    @Transactional(readOnly = true)
    public Page<FrontDeskGrievance> searchGrievances(String keyword, Pageable pageable) {
        guard.requireView(FrontDeskAccessGuard.GRIEVANCES);
        return grievances.search(guard.organizationId(), blankToNull(keyword), pageable);
    }

    public FrontDeskGrievance saveGrievance(Long id, FrontDeskGrievance body) {
        guard.requireManage(FrontDeskAccessGuard.GRIEVANCES);
        FrontDeskGrievance row = id == null ? new FrontDeskGrievance() : grievance(id);
        row.setOrganizationId(guard.organizationId());
        row.setComplainantType(body.getComplainantType());
        row.setComplainantName(body.getComplainantName());
        row.setLinkedId(body.getLinkedId());
        row.setCategory(body.getCategory());
        row.setDetails(body.getDetails());
        row.setAssigneeName(body.getAssigneeName());
        row.setStatus(StringUtils.hasText(body.getStatus()) ? body.getStatus() : "LOGGED");
        return grievances.save(row);
    }

    @Transactional(readOnly = true)
    public Page<FrontDeskCertificateRequest> searchCertificates(String keyword, Pageable pageable) {
        guard.requireView(FrontDeskAccessGuard.CERTIFICATES);
        return certificates.search(guard.organizationId(), blankToNull(keyword), pageable);
    }

    public FrontDeskCertificateRequest createCertificate(FrontDeskCertificateRequest body) {
        guard.requireManage(FrontDeskAccessGuard.CERTIFICATES);
        StudentDirectoryRow student = requireStudent(body.getStudentId());
        FrontDeskCertificateRequest row = new FrontDeskCertificateRequest();
        row.setOrganizationId(guard.organizationId());
        row.setStudentId(student.studentId());
        row.setStudentName(student.studentName());
        row.setClassName(student.className());
        row.setRollNumber(student.rollNumber());
        row.setCertificateType(body.getCertificateType());
        row.setIssuanceStatus("REQUESTED");
        if ("TRANSFER_STATUS".equals(body.getCertificateType())) {
            transfers.findByStudentId(student.studentId()).stream().findFirst().ifPresent(tc -> {
                row.setTcNumber(tc.getCertificateNumber());
                row.setTcIssuedOn(tc.getCertificateIssuedOn());
                row.setIssuanceStatus(StringUtils.hasText(tc.getCertificateNumber()) ? "APPROVED" : "REQUESTED");
            });
        }
        return certificates.save(row);
    }

    public FrontDeskCertificateRequest markCertificatePrinted(Long id) {
        guard.requireManage(FrontDeskAccessGuard.CERTIFICATES);
        FrontDeskCertificateRequest row = certificates.findById(id)
                .filter(r -> r.getOrganizationId().equals(guard.organizationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Certificate request not found"));
        if (!"APPROVED".equals(row.getIssuanceStatus())) {
            throw new BadRequestException("Print is available only after the request is approved");
        }
        row.setPrintedOn(LocalDateTime.now());
        return certificates.save(row);
    }

    public FrontDeskCertificateRequest approveCertificate(Long id) {
        guard.requireManage(FrontDeskAccessGuard.CERTIFICATES);
        FrontDeskCertificateRequest row = certificates.findById(id)
                .filter(r -> r.getOrganizationId().equals(guard.organizationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Certificate request not found"));
        row.setIssuanceStatus("APPROVED");
        return certificates.save(row);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> attendanceCheck(Long studentId) {
        guard.requireView(FrontDeskAccessGuard.GATE_PASS);
        StudentDirectoryRow student = requireStudent(studentId);
        StudentAttendance row = attendance.findByOrganizationIdAndStudentIdAndAttendanceDate(
                guard.organizationId(), studentId, LocalDate.now()).orElse(null);
        String status = row == null ? "NOT_MARKED" : row.getStatus().name();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", student.studentId());
        result.put("studentName", student.studentName());
        result.put("className", student.className());
        result.put("sectionName", student.sectionName());
        result.put("rollNumber", student.rollNumber());
        result.put("status", status);
        result.put("canIssue", "PRESENT".equals(status));
        return result;
    }

    public Map<String, Object> sendGateOtp(Long studentId) {
        guard.requireManage(FrontDeskAccessGuard.GATE_PASS);
        StudentDirectoryRow student = requireStudent(studentId);
        if (!StringUtils.hasText(student.parentMobile())) {
            throw new BadRequestException("This student has no parent mobile on file");
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        FrontDeskGateOtp otp = new FrontDeskGateOtp();
        otp.setOrganizationId(guard.organizationId());
        otp.setStudentId(studentId);
        otp.setCodeHash(sha256(code));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        otp.setVerified(false);
        otps.save(otp);
        boolean sent = sms.sendSms(student.parentMobile(), "ThinkersCave gate pass OTP: " + code);
        if (!sent) {
            throw new BadRequestException("Could not send the OTP. Use call verification instead.");
        }
        return Map.of("sent", true, "expiresInMinutes", 10);
    }

    public Map<String, Object> verifyGateOtp(Long studentId, String code) {
        guard.requireManage(FrontDeskAccessGuard.GATE_PASS);
        FrontDeskGateOtp otp = otps.findFirstByOrganizationIdAndStudentIdAndVerifiedFalseAndExpiresAtAfterOrderByExpiresAtDesc(
                guard.organizationId(), studentId, LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("OTP expired. Send a new one."));
        if (!sha256(code == null ? "" : code.trim()).equals(otp.getCodeHash())) {
            throw new BadRequestException("OTP does not match");
        }
        otp.setVerified(true);
        otps.save(otp);
        return Map.of("verified", true);
    }

    public FrontDeskGatePass issueGatePass(FrontDeskGatePass body) {
        guard.requireManage(FrontDeskAccessGuard.GATE_PASS);
        Map<String, Object> check = attendanceCheck(body.getStudentId());
        if (!Boolean.TRUE.equals(check.get("canIssue"))) {
            throw new BadRequestException("Student is " + check.get("status") + " today. Gate pass cannot be issued.");
        }
        if ("OTP".equals(body.getVerificationMode())) {
            FrontDeskGateOtp latest = otps.findFirstByOrganizationIdAndStudentIdAndVerifiedTrueOrderByExpiresAtDesc(
                    guard.organizationId(), body.getStudentId()).orElse(null);
            if (latest == null || latest.getExpiresAt().isBefore(LocalDateTime.now().minusMinutes(10))) {
                throw new BadRequestException("Verify the parent OTP before issuing the gate pass");
            }
        }
        FrontDeskGatePass row = new FrontDeskGatePass();
        row.setOrganizationId(guard.organizationId());
        row.setStudentId(body.getStudentId());
        row.setStudentName(String.valueOf(check.get("studentName")));
        row.setClassName(check.get("className") == null ? null : String.valueOf(check.get("className")));
        row.setSectionName(check.get("sectionName") == null ? null : String.valueOf(check.get("sectionName")));
        row.setReason(body.getReason());
        row.setPickupPerson(body.getPickupPerson());
        row.setVerificationMode(body.getVerificationMode());
        row.setAttendanceStatus(String.valueOf(check.get("status")));
        row.setIssuedAt(LocalDateTime.now());
        return gatePasses.save(row);
    }

    @Transactional(readOnly = true)
    public Page<FrontDeskGatePass> searchGatePasses(String keyword, Pageable pageable) {
        guard.requireView(FrontDeskAccessGuard.GATE_PASS);
        return gatePasses.search(guard.organizationId(), blankToNull(keyword), pageable);
    }

    @Transactional(readOnly = true)
    public List<StudentDirectoryRow> studentDirectory(Long classId, String keyword, Pageable pageable) {
        guard.requireView(FrontDeskAccessGuard.FRONT_DESK);
        return enrollments.searchFrontDeskDirectory(classId, blankToNull(keyword), pageable).stream()
                .map(this::toStudentRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> staffDirectory(String keyword, Pageable pageable) {
        guard.requireView(FrontDeskAccessGuard.FRONT_DESK);
        return staff.searchStaff(null, null, EmploymentStatus.ACTIVE, null, blankToNull(keyword), pageable)
                .map(this::toStaffRow);
    }

    private StudentDirectoryRow toStudentRow(StudentEnrollment enrollment) {
        Student student = enrollment.getStudent();
        String parentName = null;
        String parentMobile = null;
        List<StudentParent> links = studentParents.findByStudentStudentId(student.getStudentId());
        if (!links.isEmpty() && links.get(0).getParent() != null) {
            Parent parent = links.get(0).getParent();
            parentName = (parent.getFirstName() + " " + parent.getLastName()).trim();
            parentMobile = parent.getMobileNumber();
        }
        if (!StringUtils.hasText(parentName)) {
            parentName = student.getEmergencyContactName();
            parentMobile = student.getEmergencyContactPhone();
        }
        String className = enrollment.getClassEntity() == null ? null : enrollment.getClassEntity().getName();
        String sectionName = enrollment.getSection() == null ? null : enrollment.getSection().getName();
        String roll = StringUtils.hasText(enrollment.getRollNumber()) ? enrollment.getRollNumber() : student.getRollNumber();
        return new StudentDirectoryRow(student.getStudentId(), fullName(student), className, sectionName, roll,
                student.getAdmissionNumber(), parentName, parentMobile);
    }

    private Map<String, Object> toStaffRow(Staff member) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("staffId", member.getStaffId());
        row.put("name", (member.getFirstName() + " " + member.getLastName()).trim());
        row.put("designation", member.getDesignation());
        row.put("mobile", member.getMobileNumber());
        return row;
    }

    private StudentDirectoryRow requireStudent(Long studentId) {
        if (studentId == null) {
            throw new BadRequestException("Select a student");
        }
        return enrollments.findActiveWithClassByStudentId(studentId)
                .map(this::toStudentRow)
                .orElseGet(() -> students.findById(studentId)
                        .map(student -> new StudentDirectoryRow(student.getStudentId(), fullName(student), null, null,
                                student.getRollNumber(), student.getAdmissionNumber(),
                                student.getEmergencyContactName(), student.getEmergencyContactPhone()))
                        .orElseThrow(() -> new ResourceNotFoundException("Student not found")));
    }

    private String resolveStaffName(Long staffId, String fallback) {
        if (staffId == null) {
            return fallback;
        }
        return staff.findById(staffId)
                .map(member -> (member.getFirstName() + " " + member.getLastName()).trim())
                .orElse(fallback);
    }

    private int overstayHours(Long orgId) {
        return settings.findByOrganizationId(orgId).map(FrontDeskSetting::getOverstayHours).orElse(DEFAULT_OVERSTAY_HOURS);
    }

    private void markOverstay(FrontDeskVisitor row, int hours) {
        if (row.getOutTime() != null || row.getInTime() == null) {
            row.setOverstay(false);
            return;
        }
        row.setOverstay(Duration.between(row.getInTime(), LocalDateTime.now()).toHours() >= hours);
    }

    private FrontDeskVisitor visitor(Long id) {
        return visitors.findById(id).filter(row -> row.getOrganizationId().equals(guard.organizationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Visitor not found"));
    }

    private FrontDeskAppointment appointment(Long id) {
        return appointments.findById(id).filter(row -> row.getOrganizationId().equals(guard.organizationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    }

    private FrontDeskPostalInward inward(Long id) {
        return inwardMail.findById(id).filter(row -> row.getOrganizationId().equals(guard.organizationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Inward mail not found"));
    }

    private FrontDeskPostalOutward outward(Long id) {
        return outwardMail.findById(id).filter(row -> row.getOrganizationId().equals(guard.organizationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch not found"));
    }

    private FrontDeskCallLog call(Long id) {
        return calls.findById(id).filter(row -> row.getOrganizationId().equals(guard.organizationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Call log not found"));
    }

    private FrontDeskGrievance grievance(Long id) {
        return grievances.findById(id).filter(row -> row.getOrganizationId().equals(guard.organizationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));
    }

    private String fullName(Student student) {
        String middle = StringUtils.hasText(student.getMiddleName()) ? " " + student.getMiddleName() : "";
        return (student.getFirstName() + middle + " " + student.getLastName()).trim();
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash OTP", ex);
        }
    }

    public record StudentDirectoryRow(
            Long studentId,
            String studentName,
            String className,
            String sectionName,
            String rollNumber,
            String admissionNumber,
            String parentName,
            String parentMobile
    ) {}
}
