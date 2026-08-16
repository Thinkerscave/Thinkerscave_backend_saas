package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.response.*;
import com.thinkerscave.academics.dto.response.MyTimetableResponse.TodayEntry;
import com.thinkerscave.academics.dto.response.TeacherAcademicStructureResponse.*;
import com.thinkerscave.academics.dto.response.TeacherMyClassesResponse.*;
import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.TimetableSlotKind;
import com.thinkerscave.academics.enums.TimetableStatus;
import com.thinkerscave.academics.repository.*;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.service.AcademicsMeService;
import com.thinkerscave.academics.service.TimetableService;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicsMeServiceImpl implements AcademicsMeService {

    private final AcademicsAccessGuard accessGuard;
    private final AcademicYearRepository yearRepo;
    private final StaffRepository staffRepo;
    private final StudentRepository studentRepo;
    private final StudentEnrollmentRepository enrollmentRepo;
    private final TeacherAllocationTeacherRepository tatRepo;
    private final ClassTeacherAssignmentRepository ctaRepo;
    private final ClassSubjectMappingRepository mappingRepo;
    private final TimetableVersionRepository versionRepo;
    private final TimetableService timetableService;

    // ─── Teacher: My Classes ──────────────────────────────────────────────

    @Override
    public TeacherMyClassesResponse getTeacherMyClasses(Long academicYearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_MY_CLASSES);
        Long yearId = resolveYearId(academicYearId);
        Staff staff = resolveStaff();

        List<TeacherAllocationTeacher> allocations = tatRepo
                .findActiveByStaffAndYear(staff.getStaffId(), yearId);

        Set<Long> classTeacherSectionIds = ctaRepo
                .findByStaff_StaffIdOrderByEffectiveFromDesc(staff.getStaffId())
                .stream()
                .filter(a -> a.isActive() && a.getEffectiveTo() == null)
                .map(a -> a.getSection().getSectionId())
                .collect(Collectors.toSet());

        Map<String, ClassCard> cardMap = new LinkedHashMap<>();

        for (TeacherAllocationTeacher tat : allocations) {
            TeacherAllocation ta = tat.getTeacherAllocation();
            AcademicSection section = ta.getSection();
            AcademicClass cls = section.getAcademicClass();
            ClassSubjectMapping csm = ta.getClassSubjectMapping();

            String key = cls.getClassId() + "-" + section.getSectionId();
            ClassCard card = cardMap.computeIfAbsent(key, k -> {
                Integer studentCount = null;
                try {
                    studentCount = (int) enrollmentRepo.countBySectionSectionIdAndActiveTrue(section.getSectionId());
                } catch (Exception ignored) {
                    try {
                        studentCount = (int) enrollmentRepo.countByClassEntityClassIdAndActiveTrue(cls.getClassId());
                    } catch (Exception ignored2) {
                    }
                }
                String roomName = null;
                if (section.getDefaultResource() != null) {
                    roomName = section.getDefaultResource().getName();
                }
                return ClassCard.builder()
                        .classId(cls.getClassId())
                        .className(cls.getName())
                        .classCode(cls.getCode())
                        .sectionId(section.getSectionId())
                        .sectionName(section.getName())
                        .studentCount(studentCount)
                        .roomName(roomName)
                        .subjects(new ArrayList<>())
                        .classTeacher(classTeacherSectionIds.contains(section.getSectionId()))
                        .build();
            });

            card.getSubjects().add(SubjectSlot.builder()
                    .subjectId(csm.getSubject().getSubjectId())
                    .subjectName(csm.getSubject().getName())
                    .weeklyPeriods(csm.getWeeklyPeriods())
                    .allocationId(ta.getTeacherAllocationId())
                    .build());
        }

        List<ClassCard> cards = new ArrayList<>(cardMap.values());
        int classCount = (int) cards.stream().map(ClassCard::getClassId).distinct().count();
        int sectionCount = cards.size();
        int subjectCount = (int) cards.stream()
                .flatMap(c -> c.getSubjects().stream())
                .map(SubjectSlot::getSubjectId)
                .distinct().count();
        int weeklyPeriods = cards.stream()
                .flatMap(c -> c.getSubjects().stream())
                .mapToInt(SubjectSlot::getWeeklyPeriods)
                .sum();
        int studentCount = cards.stream()
                .map(ClassCard::getStudentCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        return TeacherMyClassesResponse.builder()
                .summary(Summary.builder()
                        .classCount(classCount)
                        .sectionCount(sectionCount)
                        .subjectCount(subjectCount)
                        .weeklyPeriods(weeklyPeriods)
                        .studentCount(studentCount)
                        .build())
                .classes(cards)
                .build();
    }

    // ─── My Timetable (role-branching) ────────────────────────────────────

    @Override
    public MyTimetableResponse getMyTimetable(Long academicYearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_MY_TIMETABLE);
        Long yearId = resolveYearId(academicYearId);

        if (isStaffRole()) {
            return buildTeacherTimetable(yearId);
        } else {
            return buildStudentTimetable(yearId);
        }
    }

    private MyTimetableResponse buildTeacherTimetable(Long yearId) {
        Staff staff = resolveStaff();
        Optional<TimetableVersion> pubVersion = versionRepo
                .findByAcademicYear_AcademicYearIdAndStatus(yearId, TimetableStatus.PUBLISHED);

        if (pubVersion.isEmpty()) {
            return MyTimetableResponse.builder()
                    .role("TEACHER")
                    .academicYearId(yearId)
                    .message("No published timetable available for this academic year")
                    .todaySchedule(Collections.emptyList())
                    .build();
        }

        TimetableVersion version = pubVersion.get();
        TimetableGridResponse grid = timetableService.getGrid(
                version.getTimetableVersionId(), "TEACHER", null, staff.getStaffId(), null);

        List<TodayEntry> today = extractTodaySchedule(grid);

        return MyTimetableResponse.builder()
                .role("TEACHER")
                .academicYearId(yearId)
                .grid(grid)
                .todaySchedule(today)
                .build();
    }

    private MyTimetableResponse buildStudentTimetable(Long yearId) {
        Student student = resolveStudent();
        StudentEnrollment enrollment = enrollmentRepo
                .findActiveWithClassByStudentId(student.getStudentId())
                .orElseThrow(() -> new BusinessException("No active enrollment found"));

        Long sectionId = enrollment.getSection() != null
                ? enrollment.getSection().getSectionId() : null;

        Optional<TimetableVersion> pubVersion = versionRepo
                .findByAcademicYear_AcademicYearIdAndStatus(yearId, TimetableStatus.PUBLISHED);

        if (pubVersion.isEmpty()) {
            return MyTimetableResponse.builder()
                    .role("STUDENT")
                    .academicYearId(yearId)
                    .message("No published timetable available for this academic year")
                    .todaySchedule(Collections.emptyList())
                    .build();
        }

        TimetableVersion version = pubVersion.get();
        TimetableGridResponse grid = timetableService.getGrid(
                version.getTimetableVersionId(), "CLASS", sectionId, null, null);

        List<TodayEntry> today = extractTodaySchedule(grid);

        return MyTimetableResponse.builder()
                .role("STUDENT")
                .academicYearId(yearId)
                .grid(grid)
                .todaySchedule(today)
                .build();
    }

    private List<TodayEntry> extractTodaySchedule(TimetableGridResponse grid) {
        if (grid == null || grid.getCells() == null) {
            return Collections.emptyList();
        }
        com.thinkerscave.academics.enums.DayOfWeek today =
                com.thinkerscave.academics.enums.DayOfWeek.valueOf(LocalDate.now().getDayOfWeek().name());

        Map<Short, TimetablePeriodResponse> periodMap = new HashMap<>();
        if (grid.getPeriods() != null) {
            for (TimetablePeriodResponse p : grid.getPeriods()) {
                periodMap.put(p.getPeriodNumber(), p);
            }
        }

        return grid.getCells().stream()
                .filter(c -> c.getDayOfWeek() == today)
                .sorted(Comparator.comparing(TimetableCellResponse::getPeriodNumber,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(c -> {
                    TimetablePeriodResponse period = periodMap.get(c.getPeriodNumber());
                    return TodayEntry.builder()
                            .periodNumber(c.getPeriodNumber() != null ? c.getPeriodNumber() : 0)
                            .periodLabel(period != null ? period.getName() : null)
                            .startTime(period != null && period.getStartTime() != null
                                    ? period.getStartTime().toString() : null)
                            .endTime(period != null && period.getEndTime() != null
                                    ? period.getEndTime().toString() : null)
                            .subjectName(c.getSubjectName())
                            .className(c.getClassName())
                            .sectionName(c.getSectionName())
                            .roomName(c.getResourceName())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ─── Teacher: Academic Structure (read-only) ──────────────────────────

    @Override
    public TeacherAcademicStructureResponse getTeacherStructure(Long academicYearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_ACADEMIC_STRUCTURE);
        Long yearId = resolveYearId(academicYearId);
        Staff staff = resolveStaff();

        AcademicYear year = yearRepo.findById(yearId)
                .orElseThrow(() -> new BusinessException("Academic year not found"));

        List<TeacherAllocationTeacher> allocations = tatRepo
                .findActiveByStaffAndYear(staff.getStaffId(), yearId);

        Map<Long, ClassNode> classNodeMap = new LinkedHashMap<>();

        for (TeacherAllocationTeacher tat : allocations) {
            TeacherAllocation ta = tat.getTeacherAllocation();
            AcademicSection section = ta.getSection();
            AcademicClass cls = section.getAcademicClass();
            ClassSubjectMapping csm = ta.getClassSubjectMapping();

            ClassNode classNode = classNodeMap.computeIfAbsent(cls.getClassId(), k ->
                    ClassNode.builder()
                            .classId(cls.getClassId())
                            .className(cls.getName())
                            .classCode(cls.getCode())
                            .sections(new ArrayList<>())
                            .build());

            SectionNode sectionNode = classNode.getSections().stream()
                    .filter(s -> s.getSectionId().equals(section.getSectionId()))
                    .findFirst()
                    .orElseGet(() -> {
                        SectionNode sn = SectionNode.builder()
                                .sectionId(section.getSectionId())
                                .sectionName(section.getName())
                                .sectionCode(section.getCode())
                                .subjects(new ArrayList<>())
                                .build();
                        classNode.getSections().add(sn);
                        return sn;
                    });

            sectionNode.getSubjects().add(SubjectNode.builder()
                    .subjectId(csm.getSubject().getSubjectId())
                    .subjectName(csm.getSubject().getName())
                    .subjectCode(csm.getSubject().getCode())
                    .weeklyPeriods(csm.getWeeklyPeriods())
                    .build());
        }

        return TeacherAcademicStructureResponse.builder()
                .academicYearId(yearId)
                .academicYearName(year.getName())
                .classes(new ArrayList<>(classNodeMap.values()))
                .build();
    }

    // ─── Student: My Academics ────────────────────────────────────────────

    @Override
    public StudentMyAcademicsResponse getStudentMyAcademics(Long academicYearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_MY_ACADEMICS);
        Student student = resolveStudent();

        StudentEnrollment enrollment = enrollmentRepo
                .findActiveWithClassByStudentId(student.getStudentId())
                .orElseThrow(() -> new BusinessException("No active enrollment found"));

        AcademicClass cls = enrollment.getClassEntity();
        AcademicSection section = enrollment.getSection();
        AcademicYear year = enrollment.getAcademicYear();

        List<ClassSubjectMapping> mappings = mappingRepo
                .findActiveWithSubjectByClassId(cls.getClassId());

        List<StudentMyAcademicsResponse.SubjectInfo> subjects = mappings.stream()
                .map(m -> StudentMyAcademicsResponse.SubjectInfo.builder()
                        .subjectId(m.getSubject().getSubjectId())
                        .subjectName(m.getSubject().getName())
                        .subjectCode(m.getSubject().getCode())
                        .weeklyPeriods(m.getWeeklyPeriods())
                        .build())
                .collect(Collectors.toList());

        StudentMyAcademicsResponse.ClassTeacherInfo ctInfo = null;
        if (section != null) {
            ctaRepo.findFirstBySection_SectionIdAndActiveTrueAndEffectiveToIsNullOrderByEffectiveFromDesc(
                    section.getSectionId()).ifPresent(cta -> {});

            Optional<ClassTeacherAssignment> cta = ctaRepo
                    .findFirstBySection_SectionIdAndActiveTrueAndEffectiveToIsNullOrderByEffectiveFromDesc(
                            section.getSectionId());
            if (cta.isPresent()) {
                Staff teacher = cta.get().getStaff();
                ctInfo = StudentMyAcademicsResponse.ClassTeacherInfo.builder()
                        .staffId(teacher.getStaffId())
                        .staffName(teacher.getFirstName() + " " + teacher.getLastName())
                        .build();
            }
        }

        Long yearId = year != null ? year.getAcademicYearId() : null;
        boolean hasTimetable = yearId != null && versionRepo
                .findByAcademicYear_AcademicYearIdAndStatus(yearId, TimetableStatus.PUBLISHED)
                .isPresent();

        return StudentMyAcademicsResponse.builder()
                .studentName(student.getFirstName() + " " + student.getLastName())
                .admissionNumber(student.getAdmissionNumber())
                .classId(cls.getClassId())
                .className(cls.getName())
                .sectionId(section != null ? section.getSectionId() : null)
                .sectionName(section != null ? section.getName() : null)
                .rollNumber(enrollment.getRollNumber())
                .academicYearId(yearId)
                .academicYearName(year != null ? year.getName() : null)
                .subjects(subjects)
                .classTeacher(ctInfo)
                .publishedTimetableExists(hasTimetable)
                .build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private Staff resolveStaff() {
        Long userId = accessGuard.currentUserIdOrNull();
        if (userId == null) {
            throw new BusinessException("Authentication required");
        }
        return staffRepo.findByUser_Id(userId)
                .orElseThrow(() -> new BusinessException("Staff profile required"));
    }

    private Student resolveStudent() {
        Long userId = accessGuard.currentUserIdOrNull();
        if (userId == null) {
            throw new BusinessException("Authentication required");
        }
        return studentRepo.findByUser_Id(userId)
                .orElseThrow(() -> new BusinessException("Student profile required"));
    }

    private Long resolveYearId(Long academicYearId) {
        if (academicYearId != null) {
            return academicYearId;
        }
        return yearRepo.findByCurrentYearTrue()
                .map(AcademicYear::getAcademicYearId)
                .orElseThrow(() -> new BusinessException("No current academic year found"));
    }

    private boolean isStaffRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "STAFF".equals(a)
                        || "SUPER_ADMIN".equals(a)
                        || "ORGANIZATION_OWNER".equals(a)
                        || "ORGANIZATION_ADMIN".equals(a));
    }
}
