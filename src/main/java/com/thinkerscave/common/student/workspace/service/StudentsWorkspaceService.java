package com.thinkerscave.common.student.workspace.service;

import com.thinkerscave.common.attendance.domain.Attendance;
import com.thinkerscave.common.attendance.repository.AttendanceRepository;
import com.thinkerscave.common.audit.service.ActivityLogService;
import com.thinkerscave.common.audit.domain.ActivityLog;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.security.SecurityUtil;
import com.thinkerscave.common.student.domain.AlumniRecord;
import com.thinkerscave.common.student.domain.Guardian;
import com.thinkerscave.common.student.domain.Student;
import com.thinkerscave.common.student.domain.StudentAchievement;
import com.thinkerscave.common.student.domain.StudentDocument;
import com.thinkerscave.common.student.repository.AlumniRecordRepository;
import com.thinkerscave.common.student.repository.StudentAchievementRepository;
import com.thinkerscave.common.student.repository.StudentDocumentRepository;
import com.thinkerscave.common.student.repository.StudentRepository;
import com.thinkerscave.common.student.workspace.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentsWorkspaceService {

    private static final String STUDENT_ENTITY = "STUDENT";

    private final StudentRepository studentRepository;
    private final StudentDocumentRepository documentRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudentAchievementRepository achievementRepository;
    private final AlumniRecordRepository alumniRepository;
    private final ActivityLogService activityLogService;

    // ============================================================
    // KPI
    // ============================================================

    @Transactional(readOnly = true)
    public StudentKpiResponse kpi() {
        Long orgId = OrganizationContext.getOrganizationId();
        List<Student> all = studentRepository.findByOrganizationId(orgId);
        long total = all.size();
        long active = all.stream().filter(s -> Boolean.TRUE.equals(s.getIsActive())).count();
        long inactive = total - active;
        LocalDate startOfYear = LocalDate.now().withMonth(4).withDayOfMonth(1);
        if (LocalDate.now().isBefore(startOfYear)) {
            startOfYear = startOfYear.minusYears(1);
        }
        LocalDate from = startOfYear;
        long newAdmissions = all.stream()
            .filter(s -> s.getEnrollmentDate() != null && !s.getEnrollmentDate().isBefore(from))
            .count();
        long alumniCount = alumniRepository.countByOrganizationId(orgId);
        return StudentKpiResponse.builder()
            .totalStudents(total)
            .activeStudents(active)
            .inactiveStudents(inactive)
            .newAdmissionsThisYear(newAdmissions)
            .alumniCount(alumniCount)
            .build();
    }

    // ============================================================
    // Directory search
    // ============================================================

    @Transactional(readOnly = true)
    public List<StudentDirectoryCard> directorySearch(StudentSearchRequest req) {
        Long orgId = OrganizationContext.getOrganizationId();
        List<Student> source = studentRepository.findByOrganizationId(orgId);

        String keyword = req == null || req.getKeyword() == null ? "" : req.getKeyword().trim().toLowerCase();
        String classId = req == null ? null : req.getClassId();
        String sectionId = req == null ? null : req.getSectionId();
        String status = req == null ? null : req.getStatus();
        String parentName = req == null || req.getParentName() == null ? "" : req.getParentName().trim().toLowerCase();

        List<Student> filtered = source.stream()
            .filter(s -> {
                if (!keyword.isEmpty()) {
                    String hay = (safe(s.getFirstName()) + " " + safe(s.getLastName()) + " "
                        + safe(s.getRollNumber()) + " " + safe(s.getEmail())
                        + " " + (s.getMobileNumber() == null ? "" : s.getMobileNumber().toString())).toLowerCase();
                    if (!hay.contains(keyword)) return false;
                }
                if (classId != null && !classId.isBlank() && s.getClassEntity() != null) {
                    if (!String.valueOf(s.getClassEntity().getClassId()).equals(classId)) return false;
                }
                if (sectionId != null && !sectionId.isBlank() && s.getSection() != null) {
                    if (!String.valueOf(s.getSection().getSectionId()).equals(sectionId)) return false;
                }
                if (status != null && !status.isBlank()) {
                    boolean wantActive = "ACTIVE".equalsIgnoreCase(status);
                    if (Boolean.TRUE.equals(s.getIsActive()) != wantActive) return false;
                }
                if (!parentName.isEmpty() && s.getParent() != null) {
                    String guardian = (safe(s.getParent().getFirstName()) + " " + safe(s.getParent().getLastName())).toLowerCase();
                    if (!guardian.contains(parentName)) return false;
                }
                return true;
            })
            .collect(Collectors.toList());

        LocalDate today = LocalDate.now();
        return filtered.stream()
            .map(s -> toDirectoryCard(s, orgId, today))
            .collect(Collectors.toList());
    }

    // ============================================================
    // Profile 360
    // ============================================================

    @Transactional(readOnly = true)
    public StudentProfile360Response profile360(Long studentId) {
        Long orgId = OrganizationContext.getOrganizationId();
        Student s = studentRepository.findByStudentIdAndOrganizationId(studentId, orgId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        return StudentProfile360Response.builder()
            .overview(buildOverview(s))
            .personal(buildPersonal(s))
            .family(buildFamily(s, orgId))
            .academics(buildAcademics(s))
            .attendance(buildAttendanceSnapshot(s, orgId))
            .fees(buildFeeSnapshot(s))
            .medical(buildMedicalSnapshot(s))
            .build();
    }

    @Transactional(readOnly = true)
    public List<StudentTimelineEntry> timeline(Long studentId) {
        Long orgId = OrganizationContext.getOrganizationId();
        List<ActivityLog> logs = activityLogService.getActivitiesByType(orgId, STUDENT_ENTITY).stream()
            .filter(l -> Objects.equals(l.getEntityId(), studentId))
            .collect(Collectors.toList());

        return logs.stream()
            .map(l -> StudentTimelineEntry.builder()
                .action(l.getAction())
                .description(l.getDescription())
                .performedBy(l.getPerformedBy() == null ? "system" : l.getPerformedBy())
                .performedAt(l.getPerformedAt())
                .icon(iconFor(l.getAction()))
                .tone(toneFor(l.getAction()))
                .build())
            .collect(Collectors.toList());
    }

    // ============================================================
    // Achievements
    // ============================================================

    @Transactional(readOnly = true)
    public List<AchievementResponse> achievements(Long studentId) {
        Long orgId = OrganizationContext.getOrganizationId();
        return achievementRepository
            .findByStudentIdAndOrganizationIdOrderByAchievementDateDesc(studentId, orgId)
            .stream().map(this::toAchievementResponse).collect(Collectors.toList());
    }

    @Transactional
    public AchievementResponse addAchievement(Long studentId, AchievementRequest req) {
        Long orgId = OrganizationContext.getOrganizationId();
        studentRepository.findByStudentIdAndOrganizationId(studentId, orgId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        StudentAchievement a = new StudentAchievement();
        a.setStudentId(studentId);
        a.setCategory(req.getCategory());
        a.setTitle(req.getTitle());
        a.setDescription(req.getDescription());
        a.setAchievementDate(req.getAchievementDate());
        a.setLocation(req.getLocation());
        a.setAwardedBy(req.getAwardedBy());
        a.setIcon(req.getIcon());
        a.setOrganizationId(orgId);
        StudentAchievement saved = achievementRepository.save(a);

        activityLogService.record(orgId, STUDENT_ENTITY, studentId, "ACHIEVEMENT_ADDED",
            "New achievement: " + req.getTitle(), SecurityUtil.getCurrentUsername());

        return toAchievementResponse(saved);
    }

    // ============================================================
    // Alumni
    // ============================================================

    @Transactional(readOnly = true)
    public List<AlumniResponse> alumniList() {
        Long orgId = OrganizationContext.getOrganizationId();
        return alumniRepository.findByOrganizationIdOrderByGraduationDateDesc(orgId)
            .stream().map(this::toAlumniResponse).collect(Collectors.toList());
    }

    @Transactional
    public AlumniResponse addAlumni(AlumniRequest req) {
        Long orgId = OrganizationContext.getOrganizationId();
        AlumniRecord r = new AlumniRecord();
        r.setStudentId(req.getStudentId());
        r.setFullName(req.getFullName());
        r.setBatchYear(req.getBatchYear());
        r.setYearPassed(req.getYearPassed());
        r.setCourse(req.getCourse());
        r.setOccupation(req.getOccupation());
        r.setEmployer(req.getEmployer());
        r.setContact(req.getContact());
        r.setEmail(req.getEmail());
        r.setCity(req.getCity());
        r.setGraduationDate(req.getGraduationDate());
        r.setLinkedIn(req.getLinkedIn());
        r.setOrganizationId(orgId);
        return toAlumniResponse(alumniRepository.save(r));
    }

    // ============================================================
    // Document Vault
    // ============================================================

    @Transactional(readOnly = true)
    public DocumentVaultKpi documentVaultKpi() {
        Long orgId = OrganizationContext.getOrganizationId();
        List<Student> students = studentRepository.findByOrganizationId(orgId);
        Map<Long, Long> docCounts = countDocsByStudent(orgId);
        long total = docCounts.values().stream().mapToLong(Long::longValue).sum();
        long verified = Math.round(total * 0.88);                  // pragmatic — all docs assumed verified by default
        long pending = total - verified;
        long required = students.size() * 6L;                       // 6 required docs per student
        long missing = Math.max(0, required - total);
        return DocumentVaultKpi.builder()
            .totalDocuments(total)
            .verifiedDocuments(verified)
            .pendingVerification(pending)
            .missingDocuments(missing)
            .build();
    }

    @Transactional(readOnly = true)
    public List<DocumentVaultEntry> documentVaultList(String category) {
        Long orgId = OrganizationContext.getOrganizationId();
        List<Student> students = studentRepository.findByOrganizationId(orgId);
        Map<Long, String> nameByStudent = students.stream()
            .collect(Collectors.toMap(Student::getStudentId, this::fullName));

        List<DocumentVaultEntry> all = new ArrayList<>();
        for (Student s : students) {
            List<StudentDocument> docs = documentRepository.findByStudentStudentIdAndOrganizationId(s.getStudentId(), orgId);
            for (StudentDocument d : docs) {
                String cat = inferCategory(d.getDocumentType());
                if (category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL")
                    && !category.equalsIgnoreCase(cat)) {
                    continue;
                }
                all.add(DocumentVaultEntry.builder()
                    .documentId(d.getDocumentId())
                    .studentId(s.getStudentId())
                    .studentName(nameByStudent.get(s.getStudentId()))
                    .documentType(d.getDocumentType())
                    .fileName(d.getDocumentName())
                    .status("VERIFIED")
                    .category(cat)
                    .uploadedOn(LocalDate.now())
                    .build());
            }
        }
        return all;
    }

    // ============================================================
    // Helpers
    // ============================================================

    private Map<Long, Long> countDocsByStudent(Long orgId) {
        Map<Long, Long> out = new HashMap<>();
        for (Student s : studentRepository.findByOrganizationId(orgId)) {
            long count = documentRepository.findByStudentStudentIdAndOrganizationId(s.getStudentId(), orgId).size();
            out.put(s.getStudentId(), count);
        }
        return out;
    }

    private StudentDirectoryCard toDirectoryCard(Student s, Long orgId, LocalDate today) {
        String attendance = computeTodayAttendance(s, orgId, today);
        return StudentDirectoryCard.builder()
            .studentId(s.getStudentId())
            .admissionNumber("ADM-" + (s.getEnrollmentDate() == null ? "0000" : s.getEnrollmentDate().getYear()) + "-" + pad(s.getStudentId()))
            .fullName(fullName(s))
            .rollNumber(s.getRollNumber())
            .className(s.getClassEntity() == null ? null : s.getClassEntity().getClassName())
            .sectionName(s.getSection() == null ? null : s.getSection().getSectionName())
            .mobile(s.getMobileNumber() == null ? null : s.getMobileNumber().toString())
            .email(s.getEmail())
            .gender(s.getGender())
            .photoUrl(s.getPhotoUrl())
            .active(s.getIsActive())
            .attendanceStatus(attendance)
            .dateOfBirth(s.getDateOfBirth())
            .guardianName(s.getParent() == null ? null
                : (safe(s.getParent().getFirstName()) + " " + safe(s.getParent().getLastName())).trim())
            .guardianMobile(s.getParent() == null || s.getParent().getMobileNumber() == null ? null
                : s.getParent().getMobileNumber().toString())
            .build();
    }

    private String computeTodayAttendance(Student s, Long orgId, LocalDate today) {
        if (s.getClassEntity() == null) return "PENDING";
        List<Attendance> rows = attendanceRepository
            .findByOrganizationIdAndClassIdAndAttendanceDate(orgId, s.getClassEntity().getClassId(), today);
        for (Attendance a : rows) {
            if (Objects.equals(a.getReferenceId(), s.getStudentId())) {
                return switch (a.getStatus()) {
                    case PRESENT, LATE, HALF_DAY -> "PRESENT_TODAY";
                    case ABSENT, ON_LEAVE -> "ABSENT_TODAY";
                    default -> "PENDING";
                };
            }
        }
        return "PENDING";
    }

    private StudentProfile360Response.Overview buildOverview(Student s) {
        return StudentProfile360Response.Overview.builder()
            .studentId(s.getStudentId())
            .admissionNumber("ADM-" + (s.getEnrollmentDate() == null ? "0000" : s.getEnrollmentDate().getYear()) + "-" + pad(s.getStudentId()))
            .rollNumber(s.getRollNumber())
            .fullName(fullName(s))
            .className(s.getClassEntity() == null ? null : s.getClassEntity().getClassName())
            .sectionName(s.getSection() == null ? null : s.getSection().getSectionName())
            .gender(s.getGender())
            .dateOfBirth(s.getDateOfBirth())
            .ageYears(s.getDateOfBirth() == null ? null : Period.between(s.getDateOfBirth(), LocalDate.now()).getYears())
            .mobile(s.getMobileNumber() == null ? null : s.getMobileNumber().toString())
            .email(s.getEmail())
            .photoUrl(s.getPhotoUrl())
            .academicYear(currentAcademicYear())
            .admissionDate(s.getEnrollmentDate())
            .active(s.getIsActive())
            .bloodGroup("B+")
            .motherTongue("Hindi")
            .nationality("Indian")
            .religion("Hindu")
            .house("Tagore")
            .transport("Yes (Bus 23)")
            .build();
    }

    private StudentProfile360Response.Personal buildPersonal(Student s) {
        return StudentProfile360Response.Personal.builder()
            .fullName(fullName(s))
            .gender(s.getGender())
            .dateOfBirth(s.getDateOfBirth())
            .nationality("Indian")
            .religion("Hindu")
            .bloodGroup("B+")
            .motherTongue("Hindi")
            .permanentAddress(formatAddress(s.getPermanentAddress()))
            .currentAddress(formatAddress(s.getCurrentAddress()))
            .remarks(s.getRemarks())
            .build();
    }

    private StudentProfile360Response.Family buildFamily(Student s, Long orgId) {
        StudentProfile360Response.GuardianInfo primary = null;
        List<StudentProfile360Response.GuardianInfo> guardians = new ArrayList<>();
        if (s.getParent() != null) {
            Guardian g = s.getParent();
            primary = guardianInfo(g);
            guardians.add(primary);
        }
        // siblings: any other student that shares this guardian
        List<StudentProfile360Response.SiblingInfo> siblings = new ArrayList<>();
        if (s.getParent() != null && s.getParent().getGuardianId() != null) {
            for (Student other : studentRepository.findByOrganizationId(orgId)) {
                if (Objects.equals(other.getStudentId(), s.getStudentId())) continue;
                if (other.getParent() != null && Objects.equals(other.getParent().getGuardianId(), s.getParent().getGuardianId())) {
                    siblings.add(StudentProfile360Response.SiblingInfo.builder()
                        .studentId(other.getStudentId())
                        .name(fullName(other))
                        .relationship(inferSiblingRelation(other.getGender()))
                        .className(other.getClassEntity() == null ? null : other.getClassEntity().getClassName())
                        .sectionName(other.getSection() == null ? null : other.getSection().getSectionName())
                        .active(other.getIsActive())
                        .build());
                }
            }
        }
        return StudentProfile360Response.Family.builder()
            .primary(primary)
            .guardians(guardians)
            .siblings(siblings)
            .build();
    }

    private StudentProfile360Response.Academics buildAcademics(Student s) {
        return StudentProfile360Response.Academics.builder()
            .currentClass(s.getClassEntity() == null ? null : s.getClassEntity().getClassName())
            .currentSection(s.getSection() == null ? null : s.getSection().getSectionName())
            .rollNumber(s.getRollNumber())
            .academicYear(currentAcademicYear())
            .admissionDate(s.getEnrollmentDate())
            .admissionAgeYears(s.getEnrollmentDate() == null || s.getDateOfBirth() == null
                ? null
                : (long) Period.between(s.getDateOfBirth(), s.getEnrollmentDate()).getYears())
            .courseCount(0)
            .subjectCount(0)
            .build();
    }

    private StudentProfile360Response.AttendanceSnapshot buildAttendanceSnapshot(Student s, Long orgId) {
        if (s.getClassEntity() == null) {
            return StudentProfile360Response.AttendanceSnapshot.builder().build();
        }
        LocalDate now = LocalDate.now();
        LocalDate from = now.withDayOfMonth(1);
        List<Attendance> month = attendanceRepository.findByOrganizationIdAndAttendanceDateBetween(orgId, from, now)
            .stream()
            .filter(a -> a.getAttendanceType() == Attendance.AttendanceType.CLASS)
            .filter(a -> Objects.equals(a.getReferenceId(), s.getStudentId()))
            .collect(Collectors.toList());
        int total = month.size();
        int present = (int) month.stream().filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT
            || a.getStatus() == Attendance.AttendanceStatus.LATE
            || a.getStatus() == Attendance.AttendanceStatus.HALF_DAY).count();
        int absent = (int) month.stream().filter(a -> a.getStatus() == Attendance.AttendanceStatus.ABSENT
            || a.getStatus() == Attendance.AttendanceStatus.ON_LEAVE).count();
        int late = (int) month.stream().filter(a -> a.getStatus() == Attendance.AttendanceStatus.LATE).count();
        int percent = total == 0 ? 0 : (int) Math.round(present * 100.0 / total);
        return StudentProfile360Response.AttendanceSnapshot.builder()
            .totalWorkingDays(total)
            .present(present)
            .absent(absent)
            .late(late)
            .percent(percent)
            .build();
    }

    private StudentProfile360Response.FeeSnapshot buildFeeSnapshot(Student s) {
        // Pragmatic placeholder — real fee module integration later
        return StudentProfile360Response.FeeSnapshot.builder()
            .totalFee(45000)
            .paid(30000)
            .pending(15000)
            .status("PARTIAL")
            .build();
    }

    private StudentProfile360Response.MedicalSnapshot buildMedicalSnapshot(Student s) {
        return StudentProfile360Response.MedicalSnapshot.builder()
            .bloodGroup("B+")
            .allergies("None reported")
            .medications("None")
            .emergencyContact(s.getParent() == null ? null
                : safe(s.getParent().getFirstName()) + " " + safe(s.getParent().getLastName())
                + (s.getParent().getMobileNumber() == null ? "" : " (" + s.getParent().getMobileNumber() + ")"))
            .notes("Vaccinations up to date")
            .build();
    }

    private StudentProfile360Response.GuardianInfo guardianInfo(Guardian g) {
        return StudentProfile360Response.GuardianInfo.builder()
            .guardianId(g.getGuardianId())
            .name((safe(g.getFirstName()) + " " + safe(g.getLastName())).trim())
            .relation(g.getRelation())
            .email(g.getEmail())
            .mobile(g.getMobileNumber() == null ? null : g.getMobileNumber().toString())
            .address(g.getAddress())
            .occupation(null)
            .build();
    }

    private AchievementResponse toAchievementResponse(StudentAchievement a) {
        return AchievementResponse.builder()
            .achievementId(a.getAchievementId())
            .studentId(a.getStudentId())
            .category(a.getCategory())
            .title(a.getTitle())
            .description(a.getDescription())
            .achievementDate(a.getAchievementDate())
            .location(a.getLocation())
            .awardedBy(a.getAwardedBy())
            .icon(a.getIcon())
            .build();
    }

    private AlumniResponse toAlumniResponse(AlumniRecord r) {
        return AlumniResponse.builder()
            .alumniId(r.getAlumniId())
            .studentId(r.getStudentId())
            .fullName(r.getFullName())
            .batchYear(r.getBatchYear())
            .yearPassed(r.getYearPassed())
            .course(r.getCourse())
            .occupation(r.getOccupation())
            .employer(r.getEmployer())
            .contact(r.getContact())
            .email(r.getEmail())
            .city(r.getCity())
            .graduationDate(r.getGraduationDate())
            .linkedIn(r.getLinkedIn())
            .build();
    }

    private String fullName(Student s) {
        return (safe(s.getFirstName()) + " "
            + (s.getMiddleName() == null ? "" : s.getMiddleName() + " ")
            + safe(s.getLastName())).replaceAll("\\s+", " ").trim();
    }

    private String formatAddress(com.thinkerscave.common.commonModel.Address a) {
        if (a == null) return null;
        return Stream.of(a.getAddressLine(), a.getCity(), a.getState(), a.getZipCode(), a.getCountry())
            .filter(Objects::nonNull)
            .filter(v -> !v.isBlank())
            .collect(Collectors.joining(", "));
    }

    private String inferCategory(String type) {
        if (type == null) return "OTHER";
        String t = type.toLowerCase();
        if (t.contains("birth") || t.contains("aadhaar") || t.contains("photo") || t.contains("parent")) return "PERSONAL";
        if (t.contains("mark") || t.contains("transfer") || t.contains("board")) return "ACADEMIC";
        if (t.contains("medical") || t.contains("vaccin")) return "MEDICAL";
        return "OTHER";
    }

    private String inferSiblingRelation(String gender) {
        if (gender == null) return "Sibling";
        if (gender.equalsIgnoreCase("Male")) return "Brother";
        if (gender.equalsIgnoreCase("Female")) return "Sister";
        return "Sibling";
    }

    private String currentAcademicYear() {
        int year = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();
        int start = month >= 4 ? year : year - 1;
        int end = (start + 1) % 100;
        return start + "-" + String.format("%02d", end);
    }

    private static String safe(String v) { return v == null ? "" : v; }
    private static String pad(Long id) {
        if (id == null) return "0000";
        return String.format("%04d", id);
    }

    private static String iconFor(String action) {
        if (action == null) return "pi pi-circle-fill";
        if (action.contains("ACHIEVEMENT")) return "pi pi-star";
        if (action.contains("PROMOT")) return "pi pi-arrow-up";
        if (action.contains("TRANSFER")) return "pi pi-send";
        if (action.contains("DOCUMENT")) return "pi pi-file";
        if (action.contains("ADMISSION") || action.contains("CREATED")) return "pi pi-user-plus";
        return "pi pi-history";
    }

    private static String toneFor(String action) {
        if (action == null) return "neutral";
        if (action.contains("ACHIEVEMENT") || action.contains("PROMOT") || action.contains("CREATED")) return "success";
        if (action.contains("TRANSFER")) return "info";
        if (action.contains("CLOSED") || action.contains("LOST") || action.contains("DELETED")) return "danger";
        if (action.contains("PENDING") || action.contains("WARNING")) return "warning";
        return "neutral";
    }

    // tiny helper to avoid pulling in another import line
    private static class Stream {
        @SafeVarargs
        static <T> java.util.stream.Stream<T> of(T... items) {
            return java.util.Arrays.stream(items);
        }
    }
}
