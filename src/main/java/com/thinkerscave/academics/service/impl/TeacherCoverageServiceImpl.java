package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.TeacherCoverageSaveRequest;
import com.thinkerscave.academics.dto.request.TeacherCoverageSaveRequest.Change;
import com.thinkerscave.academics.dto.request.TeacherCoverageSaveRequest.CoverageAction;
import com.thinkerscave.academics.dto.response.TeacherCandidateResponse;
import com.thinkerscave.academics.dto.response.TeacherCoverageBoardResponse;
import com.thinkerscave.academics.dto.response.TeacherCoverageBoardResponse.Row;
import com.thinkerscave.academics.dto.response.TeacherCoverageBoardResponse.SectionAssignment;
import com.thinkerscave.academics.dto.response.TeacherCoverageBoardResponse.SectionOption;
import com.thinkerscave.academics.dto.response.TeacherCoverageBoardResponse.Summary;
import com.thinkerscave.academics.dto.response.TeacherCoverageBoardResponse.TeacherRef;
import com.thinkerscave.academics.dto.response.TeacherCoveragePreviewResponse;
import com.thinkerscave.academics.dto.response.TeacherCoveragePreviewResponse.AffectedSection;
import com.thinkerscave.academics.dto.response.TeacherCoveragePreviewResponse.Impact;
import com.thinkerscave.academics.dto.response.TeacherCoveragePreviewResponse.WorkloadWarning;
import com.thinkerscave.academics.entity.AcademicGroup;
import com.thinkerscave.academics.entity.AcademicGroupSubjectMapping;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.TeacherAllocation;
import com.thinkerscave.academics.entity.TeacherAllocationTeacher;
import com.thinkerscave.academics.entity.TeacherGroupAllocation;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.enums.TeacherAllocationStatus;
import com.thinkerscave.academics.enums.TeacherAllocationTeacherRole;
import com.thinkerscave.academics.repository.AcademicGroupRepository;
import com.thinkerscave.academics.repository.AcademicGroupSubjectMappingRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.academics.repository.TeacherAllocationRepository;
import com.thinkerscave.academics.repository.TeacherAllocationTeacherRepository;
import com.thinkerscave.academics.repository.TeacherGroupAllocationRepository;
import com.thinkerscave.academics.repository.TimetableConfigurationRepository;
import com.thinkerscave.academics.repository.TimetableEntryRepository;
import com.thinkerscave.academics.repository.TimetableConflictRepository;
import com.thinkerscave.academics.repository.TimetableVersionRepository;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.service.TeacherCoverageService;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ConflictException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.enums.StaffType;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherCoverageServiceImpl implements TeacherCoverageService {

    static final int FALLBACK_MAX_WEEKLY_PERIODS = 24;

    private static final Set<AcademicYearStatus> READ_ONLY_YEAR_STATUSES = EnumSet.of(
            AcademicYearStatus.COMPLETED, AcademicYearStatus.ARCHIVED);

    private final AcademicGroupRepository academicGroupRepository;
    private final SectionRepository sectionRepository;
    private final AcademicGroupSubjectMappingRepository mappingRepository;
    private final TeacherAllocationRepository allocationRepository;
    private final TeacherAllocationTeacherRepository allocationTeacherRepository;
    private final TeacherGroupAllocationRepository groupAllocationRepository;
    private final TimetableConfigurationRepository configurationRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final TimetableConflictRepository timetableConflictRepository;
    private final TimetableVersionRepository timetableVersionRepository;
    private final StaffRepository staffRepository;
    private final AcademicsAccessGuard accessGuard;

    enum Source { NONE, INHERITED, OVERRIDE }

    @Override
    @Transactional(readOnly = true)
    public TeacherCoverageBoardResponse getBoard(Long academicYearId, Long academicGroupId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TEACHER_ALLOCATION);
        Context ctx = load(academicYearId, academicGroupId);
        return buildBoard(ctx);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherCandidateResponse> getCandidates(Long academicYearId, Long academicGroupId,
                                                        Long mappingId, Long sectionId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TEACHER_ALLOCATION);
        Context ctx = load(academicYearId, academicGroupId);
        AcademicGroupSubjectMapping mapping = requireMapping(ctx, mappingId);
        List<Slot> scope = ctx.slotsOf(mappingId);
        if (sectionId != null) {
            scope = scope.stream().filter(s -> s.section().getSectionId().equals(sectionId)).toList();
            if (scope.isEmpty()) {
                throw new BusinessException("This section is not part of the selected academic group.");
            }
        }
        int weekly = weeklyOf(mapping);
        int units = scope.isEmpty() ? 1 : scope.size();
        int added = weekly * units;
        Long subjectId = mapping.getSubject().getSubjectId();

        List<TeacherCandidateResponse> result = new ArrayList<>();
        for (Staff staff : teachingStaff()) {
            long heldSlots = scope.isEmpty()
                    ? (ctx.currentGroupStaff.get(mapping.getAcademicGroupSubjectMappingId()) != null
                        && ctx.currentGroupStaff.get(mapping.getAcademicGroupSubjectMappingId()).getStaffId().equals(staff.getStaffId()) ? 1 : 0)
                    : scope.stream()
                        .filter(s -> ctx.currentStaff.get(s.key()) != null
                                && ctx.currentStaff.get(s.key()).getStaffId().equals(staff.getStaffId()))
                        .count();
            int current = ctx.yearLoad.getOrDefault(staff.getStaffId(), 0);
            int projected = current - (int) heldSlots * weekly + added;
            result.add(TeacherCandidateResponse.builder()
                    .staffId(staff.getStaffId())
                    .name(name(staff))
                    .staffCode(staff.getStaffCode())
                    .primarySubjectName(staff.getPrimarySubject() == null ? null : staff.getPrimarySubject().getName())
                    .secondarySubjectName(staff.getSecondarySubject() == null ? null : staff.getSecondarySubject().getName())
                    .preferenceRank(preferenceRank(
                            staff.getPrimarySubject() == null ? null : staff.getPrimarySubject().getSubjectId(),
                            staff.getSecondarySubject() == null ? null : staff.getSecondarySubject().getSubjectId(),
                            subjectId))
                    .currentPeriods(current)
                    .projectedPeriods(projected)
                    .maxPeriods(ctx.maxPeriods)
                    .workloadStatus(workloadStatus(projected, ctx.maxPeriods))
                    .build());
        }
        result.sort(Comparator.comparingInt(TeacherCandidateResponse::getPreferenceRank)
                .thenComparingInt(TeacherCandidateResponse::getProjectedPeriods)
                .thenComparing(TeacherCandidateResponse::getName));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherCoveragePreviewResponse preview(Long academicYearId, TeacherCoverageSaveRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TEACHER_ALLOCATION);
        return toPreview(plan(academicYearId, request));
    }

    @Override
    public TeacherCoverageBoardResponse save(Long academicYearId, TeacherCoverageSaveRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TEACHER_ALLOCATION);
        Plan plan = plan(academicYearId, request);
        if (plan.requiresConfirmation() && !request.isConfirmImpact()) {
            throw new ConflictException("This change exceeds a teacher's weekly capacity or changes timetable lessons. "
                    + "Review the impact and confirm to continue.");
        }
        write(plan);
        return buildBoard(load(academicYearId, request.getAcademicGroupId()));
    }

    // ----- planning -----

    private Plan plan(Long academicYearId, TeacherCoverageSaveRequest request) {
        if (request.getChanges() == null || request.getChanges().isEmpty()) {
            throw new BusinessException("There are no teacher changes to save.");
        }
        Context ctx = load(academicYearId, request.getAcademicGroupId());
        assertYearMutable(ctx.year);
        for (Change change : request.getChanges()) {
            applyChange(ctx, change);
        }

        List<AffectedSection> affected = new ArrayList<>();
        Map<Long, Long> lessonsByMapping = new LinkedHashMap<>();
        for (Slot slot : ctx.slots.values()) {
            Staff before = ctx.currentStaff.get(slot.key());
            Staff after = ctx.workingStaff.get(slot.key());
            Source beforeSource = ctx.currentSource.get(slot.key());
            Source afterSource = ctx.workingSource.get(slot.key());
            if (Objects.equals(idOf(before), idOf(after)) && beforeSource == afterSource) continue;
            affected.add(AffectedSection.builder()
                    .mappingId(slot.mapping().getAcademicGroupSubjectMappingId())
                    .subjectName(slot.mapping().getSubject().getName())
                    .sectionId(slot.section().getSectionId())
                    .sectionName(slot.section().getName())
                    .fromTeacher(before == null ? null : name(before))
                    .toTeacher(after == null ? null : name(after))
                    .build());
            TeacherAllocation existing = ctx.currentAllocation.get(slot.key());
            if (existing != null) {
                long entries = timetableEntryRepository.countByTeacherAllocation_TeacherAllocationId(
                        existing.getTeacherAllocationId());
                if (entries > 0) {
                    lessonsByMapping.merge(slot.mapping().getAcademicGroupSubjectMappingId(), entries, Long::sum);
                }
            }
        }

        List<Impact> impacts = new ArrayList<>();
        for (Map.Entry<Long, Integer> kept : ctx.keptOverrides.entrySet()) {
            AcademicGroupSubjectMapping mapping = ctx.mappings.get(kept.getKey());
            impacts.add(Impact.builder()
                    .mappingId(kept.getKey())
                    .subjectName(mapping.getSubject().getName())
                    .type("SECTION_OVERRIDES_KEPT")
                    .count(kept.getValue())
                    .message(kept.getValue() + " section(s) keep their own teacher and are not changed.")
                    .build());
        }
        for (Map.Entry<Long, Long> lessons : lessonsByMapping.entrySet()) {
            AcademicGroupSubjectMapping mapping = ctx.mappings.get(lessons.getKey());
            impacts.add(Impact.builder()
                    .mappingId(lessons.getKey())
                    .subjectName(mapping.getSubject().getName())
                    .type("TIMETABLE_LESSONS")
                    .count(lessons.getValue())
                    .message(lessons.getValue() + " timetable lesson(s) for " + mapping.getSubject().getName()
                            + " will use the new teacher. Review the timetable after saving.")
                    .build());
        }

        Set<Long> incoming = new LinkedHashSet<>();
        ctx.workingStaff.values().stream().filter(Objects::nonNull).forEach(s -> incoming.add(s.getStaffId()));
        ctx.workingGroupStaff.values().stream().filter(Objects::nonNull).forEach(s -> incoming.add(s.getStaffId()));
        List<WorkloadWarning> warnings = new ArrayList<>();
        for (Long staffId : incoming) {
            int projected = ctx.yearLoad.getOrDefault(staffId, 0)
                    - groupLoad(ctx.currentStaff, ctx.currentGroupStaff, ctx, staffId)
                    + groupLoad(ctx.workingStaff, ctx.workingGroupStaff, ctx, staffId);
            if (projected > ctx.maxPeriods && projected > ctx.yearLoad.getOrDefault(staffId, 0)) {
                Staff staff = ctx.staffById.get(staffId);
                warnings.add(WorkloadWarning.builder()
                        .staffId(staffId)
                        .staffName(staff == null ? String.valueOf(staffId) : name(staff))
                        .projectedPeriods(projected)
                        .maxPeriods(ctx.maxPeriods)
                        .build());
            }
        }

        boolean requiresConfirmation = !impacts.isEmpty() || !warnings.isEmpty();
        return new Plan(ctx, affected, warnings, impacts, requiresConfirmation);
    }

    private void applyChange(Context ctx, Change change) {
        AcademicGroupSubjectMapping mapping = requireMapping(ctx, change.getMappingId());
        Long mappingId = mapping.getAcademicGroupSubjectMappingId();
        List<Slot> scope = ctx.slotsOf(mappingId);

        switch (change.getAction()) {
            case ASSIGN_GROUP -> {
                Staff staff = requireTeacher(change.getStaffId());
                assertSubjectActive(mapping);
                ctx.workingGroupStaff.put(mappingId, staff);
                for (Slot slot : scope) {
                    if (ctx.workingSource.get(slot.key()) == Source.OVERRIDE && !change.isReplaceOverrides()) {
                        ctx.keptOverrides.merge(mappingId, 1, Integer::sum);
                        continue;
                    }
                    ctx.workingStaff.put(slot.key(), staff);
                    ctx.workingSource.put(slot.key(), Source.INHERITED);
                }
            }
            case CLEAR_GROUP -> {
                ctx.workingGroupStaff.put(mappingId, null);
                for (Slot slot : scope) {
                    if (ctx.workingSource.get(slot.key()) == Source.INHERITED) {
                        ctx.workingStaff.put(slot.key(), null);
                        ctx.workingSource.put(slot.key(), Source.NONE);
                    }
                }
            }
            case ASSIGN_SECTIONS -> {
                Staff staff = requireTeacher(change.getStaffId());
                assertSubjectActive(mapping);
                for (Long sectionId : requireSectionIds(change)) {
                    Slot slot = requireSlot(ctx, mappingId, sectionId);
                    ctx.workingStaff.put(slot.key(), staff);
                    ctx.workingSource.put(slot.key(), Source.OVERRIDE);
                }
            }
            case CLEAR_SECTIONS -> {
                for (Long sectionId : requireSectionIds(change)) {
                    Slot slot = requireSlot(ctx, mappingId, sectionId);
                    if (ctx.workingSource.get(slot.key()) != Source.OVERRIDE) continue;
                    Staff inherited = ctx.workingGroupStaff.get(mappingId);
                    ctx.workingStaff.put(slot.key(), inherited);
                    ctx.workingSource.put(slot.key(), inherited == null ? Source.NONE : Source.INHERITED);
                }
            }
        }
    }

    private void write(Plan plan) {
        Context ctx = plan.ctx();
        LocalDate today = LocalDate.now();

        for (Map.Entry<Long, Staff> entry : ctx.workingGroupStaff.entrySet()) {
            Long mappingId = entry.getKey();
            Staff before = ctx.currentGroupStaff.get(mappingId);
            Staff after = entry.getValue();
            if (Objects.equals(idOf(before), idOf(after))) continue;
            Optional<TeacherGroupAllocation> existing = groupAllocationRepository
                    .findByMapping_AcademicGroupSubjectMappingId(mappingId);
            if (after == null) {
                existing.ifPresent(row -> {
                    row.setActive(false);
                    groupAllocationRepository.save(row);
                });
                continue;
            }
            TeacherGroupAllocation row = existing.orElseGet(() -> {
                TeacherGroupAllocation created = new TeacherGroupAllocation();
                created.setMapping(ctx.mappings.get(mappingId));
                return created;
            });
            row.setStaff(after);
            row.setActive(true);
            groupAllocationRepository.save(row);
        }

        for (Slot slot : ctx.slots.values()) {
            Staff before = ctx.currentStaff.get(slot.key());
            Staff after = ctx.workingStaff.get(slot.key());
            Source beforeSource = ctx.currentSource.get(slot.key());
            Source afterSource = ctx.workingSource.get(slot.key());
            if (Objects.equals(idOf(before), idOf(after)) && beforeSource == afterSource) continue;

            TeacherAllocation allocation = allocationRepository
                    .findBySection_SectionIdAndAcademicGroupSubjectMapping_AcademicGroupSubjectMappingId(
                            slot.section().getSectionId(), slot.mapping().getAcademicGroupSubjectMappingId())
                    .orElseGet(() -> {
                        TeacherAllocation created = new TeacherAllocation();
                        created.setSection(slot.section());
                        created.setAcademicGroupSubjectMapping(slot.mapping());
                        return created;
                    });
            if (after == null) {
                if (allocation.getTeacherAllocationId() == null) continue;
                allocation.setActive(false);
                allocation.setStatus(TeacherAllocationStatus.UNASSIGNED);
                TeacherAllocation saved = allocationRepository.save(allocation);
                closePrimary(saved, today);
                continue;
            }
            allocation.setActive(true);
            allocation.setInherited(afterSource == Source.INHERITED);
            allocation.setStatus(TeacherAllocationStatus.ASSIGNED);
            TeacherAllocation saved = allocationRepository.save(allocation);
            assignPrimary(saved, after, today);
        }
    }

    private void assignPrimary(TeacherAllocation allocation, Staff staff, LocalDate today) {
        Optional<TeacherAllocationTeacher> open = allocationTeacherRepository
                .findByTeacherAllocation_TeacherAllocationIdAndRoleAndEffectiveToIsNull(
                        allocation.getTeacherAllocationId(), TeacherAllocationTeacherRole.PRIMARY);
        if (open.isPresent() && open.get().getStaff().getStaffId().equals(staff.getStaffId())) {
            return;
        }
        open.ifPresent(row -> {
            row.setEffectiveTo(today);
            row.setActive(false);
            allocationTeacherRepository.save(row);
        });
        TeacherAllocationTeacher assignment = new TeacherAllocationTeacher();
        assignment.setTeacherAllocation(allocation);
        assignment.setStaff(staff);
        assignment.setRole(TeacherAllocationTeacherRole.PRIMARY);
        assignment.setEffectiveFrom(today);
        assignment.setActive(true);
        allocationTeacherRepository.save(assignment);
    }

    private void closePrimary(TeacherAllocation allocation, LocalDate today) {
        allocationTeacherRepository
                .findByTeacherAllocation_TeacherAllocationIdAndRoleAndEffectiveToIsNull(
                        allocation.getTeacherAllocationId(), TeacherAllocationTeacherRole.PRIMARY)
                .ifPresent(row -> {
                    row.setEffectiveTo(today);
                    row.setActive(false);
                    allocationTeacherRepository.save(row);
                });
    }

    // ----- loading -----

    private Context load(Long academicYearId, Long academicGroupId) {
        AcademicGroup group = academicGroupRepository.findByIdWithYear(academicGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic Group not found: " + academicGroupId));
        AcademicYear year = group.getAcademicYear();
        if (!Objects.equals(year.getAcademicYearId(), academicYearId)) {
            throw new BusinessException("This academic group belongs to a different academic year.");
        }

        Context ctx = new Context();
        ctx.group = group;
        ctx.year = year;
        ctx.sections = sectionRepository.findByAcademicGroup_AcademicGroupIdAndActiveTrueOrderByDisplayOrderAsc(academicGroupId);
        for (AcademicGroupSubjectMapping mapping : mappingRepository
                .findByAcademicGroup_AcademicGroupIdAndActiveTrue(academicGroupId)) {
            ctx.mappings.put(mapping.getAcademicGroupSubjectMappingId(), mapping);
            for (AcademicSection section : ctx.sections) {
                Slot slot = new Slot(section, mapping);
                ctx.slots.put(slot.key(), slot);
            }
        }

        Map<Long, TeacherAllocationTeacher> primaryByAllocation = new HashMap<>();
        for (TeacherAllocationTeacher row : allocationTeacherRepository.findActiveByYear(academicYearId)) {
            if (row.getRole() != TeacherAllocationTeacherRole.PRIMARY) continue;
            primaryByAllocation.merge(row.getTeacherAllocation().getTeacherAllocationId(), row,
                    (a, b) -> effectiveFrom(b).isAfter(effectiveFrom(a)) ? b : a);
        }

        for (TeacherAllocation allocation : allocationRepository.findActiveWithDetailsByYear(academicYearId)) {
            AcademicGroupSubjectMapping mapping = allocation.getAcademicGroupSubjectMapping();
            Staff staff = Optional.ofNullable(primaryByAllocation.get(allocation.getTeacherAllocationId()))
                    .map(TeacherAllocationTeacher::getStaff).orElse(null);
            if (staff != null) {
                ctx.staffById.put(staff.getStaffId(), staff);
                if (mapping.isActive() && allocation.getSection().isActive()) {
                    ctx.yearLoad.merge(staff.getStaffId(), weeklyOf(mapping), Integer::sum);
                }
            }
            if (ctx.mappings.containsKey(mapping.getAcademicGroupSubjectMappingId())
                    && allocation.getSection().getAcademicGroup().getAcademicGroupId().equals(academicGroupId)) {
                Slot slot = ctx.slots.get(key(allocation.getSection().getSectionId(), mapping.getAcademicGroupSubjectMappingId()));
                if (slot == null) continue;
                ctx.currentAllocation.put(slot.key(), allocation);
                if (staff != null && allocation.isActive()) {
                    ctx.currentStaff.put(slot.key(), staff);
                    ctx.currentSource.put(slot.key(), allocation.getInherited()
                            ? Source.INHERITED : Source.OVERRIDE);
                } else {
                    ctx.currentSource.put(slot.key(), Source.NONE);
                }
            }
        }

        for (TeacherGroupAllocation row : groupAllocationRepository.findActiveByYear(academicYearId)) {
            Long mappingId = row.getMapping().getAcademicGroupSubjectMappingId();
            Long groupId = row.getMapping().getAcademicGroup().getAcademicGroupId();
            if (ctx.mappings.containsKey(mappingId)) {
                ctx.currentGroupStaff.put(mappingId, row.getStaff());
                ctx.staffById.put(row.getStaff().getStaffId(), row.getStaff());
            }
            if (sectionRepository.findByAcademicGroup_AcademicGroupIdAndActiveTrueOrderByDisplayOrderAsc(groupId).isEmpty()
                    && row.getMapping().isActive()) {
                ctx.yearLoad.merge(row.getStaff().getStaffId(), weeklyOf(row.getMapping()), Integer::sum);
            }
        }

        for (Slot slot : ctx.slots.values()) {
            ctx.currentStaff.putIfAbsent(slot.key(), null);
            ctx.currentSource.putIfAbsent(slot.key(), Source.NONE);
        }
        for (Long mappingId : ctx.mappings.keySet()) {
            ctx.currentGroupStaff.putIfAbsent(mappingId, null);
        }

        ctx.maxPeriods = resolveMaxWeeklyPeriods(academicYearId);
        ctx.workingStaff.putAll(ctx.currentStaff);
        ctx.workingSource.putAll(ctx.currentSource);
        ctx.workingGroupStaff.putAll(ctx.currentGroupStaff);
        return ctx;
    }

    private TeacherCoverageBoardResponse buildBoard(Context ctx) {
        List<Row> rows = ctx.mappings.values().stream()
                .sorted(Comparator.comparing(AcademicGroupSubjectMapping::getSubject, SubjectOrdering.SUBJECTS))
                .map(mapping -> buildRow(ctx, mapping))
                .toList();

        long workloadWarnings = ctx.yearLoad.values().stream().filter(load -> load > ctx.maxPeriods).count();
        return TeacherCoverageBoardResponse.builder()
                .academicYearId(ctx.year.getAcademicYearId())
                .academicYearName(ctx.year.getName())
                .academicYearStatus(ctx.year.getStatus())
                .yearReadOnly(READ_ONLY_YEAR_STATUSES.contains(ctx.year.getStatus()))
                .academicGroupId(ctx.group.getAcademicGroupId())
                .academicGroupName(ctx.group.getName())
                .academicGroupCode(ctx.group.getCode())
                .hasSections(!ctx.sections.isEmpty())
                .sections(ctx.sections.stream().map(s -> SectionOption.builder()
                        .sectionId(s.getSectionId()).name(s.getName()).code(s.getCode()).build()).toList())
                .summary(Summary.builder()
                        .subjectsRequiringTeachers(rows.stream().filter(r -> !"ASSIGNED".equals(r.getStatus())).count())
                        .fullyAllocated(rows.stream().filter(r -> "ASSIGNED".equals(r.getStatus())).count())
                        .workloadWarnings(workloadWarnings)
                        .timetableConflicts(null)
                        .timetableConflictStatus("AVAILABLE_AFTER_TIMETABLE_GENERATION")
                        .build())
                .rows(rows)
                .build();
    }

    private long resolveTimetableConflicts(Context ctx) {
        return timetableVersionRepository
                .findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(ctx.year.getAcademicYearId())
                .stream().findFirst()
                .map(v -> timetableConflictRepository
                        .countByTimetableVersion_TimetableVersionIdAndBlockingTrueAndStatusAndSection_AcademicGroup_AcademicGroupId(
                                v.getTimetableVersionId(),
                                com.thinkerscave.academics.enums.TimetableConflictStatus.OPEN,
                                ctx.group.getAcademicGroupId()))
                .orElse(0L);
    }

    private String resolveTimetableConflictStatus(Context ctx) {
        boolean generated = timetableVersionRepository
                .findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(ctx.year.getAcademicYearId())
                .stream().findFirst().isPresent();
        return generated ? "AVAILABLE" : "NOT_GENERATED";
    }

    private Row buildRow(Context ctx, AcademicGroupSubjectMapping mapping) {
        Long mappingId = mapping.getAcademicGroupSubjectMappingId();
        List<Slot> scope = ctx.slotsOf(mappingId);
        Staff group = ctx.currentGroupStaff.get(mappingId);
        if (scope.isEmpty()) {
            return buildWholeGroupRow(ctx, mapping, group);
        }
        List<SectionAssignment> sections = new ArrayList<>();
        Map<Long, Staff> distinct = new LinkedHashMap<>();
        for (Slot slot : scope) {
            Staff staff = ctx.currentStaff.get(slot.key());
            if (staff != null) distinct.put(staff.getStaffId(), staff);
            sections.add(SectionAssignment.builder()
                    .sectionId(slot.section().getSectionId())
                    .sectionName(slot.section().getName())
                    .teacher(staff == null ? null : teacherRef(staff))
                    .source(ctx.currentSource.get(slot.key()).name())
                    .build());
        }
        int assigned = (int) sections.stream().filter(s -> s.getTeacher() != null).count();
        String status = assigned == 0 ? "UNASSIGNED" : assigned == scope.size() ? "ASSIGNED" : "PARTIAL";
        Staff single = distinct.size() == 1 ? distinct.values().iterator().next() : null;
        return Row.builder()
                .mappingId(mappingId)
                .subjectId(mapping.getSubject().getSubjectId())
                .subjectName(mapping.getSubject().getName())
                .subjectCode(mapping.getSubject().getCode())
                .subjectType(mapping.getSubject().getSubjectType())
                .weeklyPeriods(mapping.getWeeklyPeriods())
                .requirement(mapping.getRequirement())
                .sectionsTotal(scope.size())
                .sectionsAssigned(assigned)
                .groupTeacher(group == null ? null : teacherRef(group))
                .assignedTeacher(single == null ? null : teacherRef(single))
                .multipleTeachers(distinct.size() > 1)
                .status(status)
                .workloadStatus(single == null ? null
                        : workloadStatus(ctx.yearLoad.getOrDefault(single.getStaffId(), 0), ctx.maxPeriods))
                .sections(sections)
                .build();
    }

    /** Groups without sections are taught as one unit, so the group teacher is the whole assignment. */
    private Row buildWholeGroupRow(Context ctx, AcademicGroupSubjectMapping mapping, Staff group) {
        return Row.builder()
                .mappingId(mapping.getAcademicGroupSubjectMappingId())
                .subjectId(mapping.getSubject().getSubjectId())
                .subjectName(mapping.getSubject().getName())
                .subjectCode(mapping.getSubject().getCode())
                .subjectType(mapping.getSubject().getSubjectType())
                .weeklyPeriods(mapping.getWeeklyPeriods())
                .requirement(mapping.getRequirement())
                .sectionsTotal(1)
                .sectionsAssigned(group == null ? 0 : 1)
                .groupTeacher(group == null ? null : teacherRef(group))
                .assignedTeacher(group == null ? null : teacherRef(group))
                .multipleTeachers(false)
                .status(group == null ? "UNASSIGNED" : "ASSIGNED")
                .workloadStatus(group == null ? null
                        : workloadStatus(ctx.yearLoad.getOrDefault(group.getStaffId(), 0), ctx.maxPeriods))
                .sections(List.of())
                .build();
    }


    // ----- helpers -----

    private int groupLoad(Map<String, Staff> staffByKey, Map<Long, Staff> groupStaff, Context ctx, Long staffId) {
        int load = 0;
        for (Slot slot : ctx.slots.values()) {
            Staff staff = staffByKey.get(slot.key());
            if (staff != null && staff.getStaffId().equals(staffId)) {
                load += weeklyOf(slot.mapping());
            }
        }
        for (AcademicGroupSubjectMapping mapping : ctx.mappings.values()) {
            Staff staff = groupStaff.get(mapping.getAcademicGroupSubjectMappingId());
            if (staff != null && staff.getStaffId().equals(staffId) && ctx.slotsOf(mapping.getAcademicGroupSubjectMappingId()).isEmpty()) {
                load += weeklyOf(mapping);
            }
        }
        return load;
    }

    private AcademicGroupSubjectMapping requireMapping(Context ctx, Long mappingId) {
        AcademicGroupSubjectMapping mapping = ctx.mappings.get(mappingId);
        if (mapping == null) {
            throw new BusinessException("This subject is not mapped to the selected academic group.");
        }
        return mapping;
    }

    private Slot requireSlot(Context ctx, Long mappingId, Long sectionId) {
        Slot slot = ctx.slots.get(key(sectionId, mappingId));
        if (slot == null) {
            throw new BusinessException("The selected section is not part of this academic group.");
        }
        return slot;
    }

    private List<Long> requireSectionIds(Change change) {
        List<Long> ids = change.getSectionIds() == null ? List.of()
                : change.getSectionIds().stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            throw new BusinessException("Select at least one section.");
        }
        return ids;
    }

    private void assertSubjectActive(AcademicGroupSubjectMapping mapping) {
        if (!mapping.getSubject().isActive()) {
            throw new BusinessException("'" + mapping.getSubject().getName()
                    + "' is inactive and cannot be assigned a teacher.");
        }
    }

    private Staff requireTeacher(Long staffId) {
        if (staffId == null) {
            throw new BusinessException("Select a teacher.");
        }
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));
        if (!Boolean.TRUE.equals(staff.getActive()) || staff.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new BusinessException(name(staff) + " is inactive and cannot be assigned.");
        }
        if (staff.getStaffType() != null && staff.getStaffType() != StaffType.TEACHING) {
            throw new BusinessException(name(staff) + " is not a teaching staff member.");
        }
        return staff;
    }

    private List<Staff> teachingStaff() {
        return staffRepository.findByActiveTrueAndEmploymentStatus(EmploymentStatus.ACTIVE).stream()
                .filter(s -> s.getStaffType() == null || s.getStaffType() == StaffType.TEACHING)
                .toList();
    }

    private int resolveMaxWeeklyPeriods(Long yearId) {
        return configurationRepository.findByAcademicYear_AcademicYearId(yearId).stream()
                .filter(c -> c.isActive())
                .map(c -> c.getMaxTeacherWeeklyPeriods())
                .filter(Objects::nonNull)
                .mapToInt(Short::intValue)
                .max()
                .orElse(FALLBACK_MAX_WEEKLY_PERIODS);
    }

    private void assertYearMutable(AcademicYear year) {
        if (READ_ONLY_YEAR_STATUSES.contains(year.getStatus())) {
            throw new BusinessException("Historical academic years are read-only");
        }
        if (!year.isActive()) {
            throw new BusinessException("Cannot modify teacher allocation for an inactive academic year");
        }
    }

    /** Teacher's relation to a subject: 1 = primary subject, 2 = secondary subject, 3 = other eligible teacher. */
    static int preferenceRank(Long primarySubjectId, Long secondarySubjectId, Long subjectId) {
        if (subjectId == null) return 3;
        if (subjectId.equals(primarySubjectId)) return 1;
        if (subjectId.equals(secondarySubjectId)) return 2;
        return 3;
    }

    static String workloadStatus(int assigned, int max) {
        if (assigned > max) return "EXCEEDS_LIMIT";
        if (assigned == max) return "AT_CAPACITY";
        if (assigned >= max * 0.85) return "LIMITED";
        return "AVAILABLE";
    }

    static int weeklyOf(AcademicGroupSubjectMapping mapping) {
        return mapping.getWeeklyPeriods() == null ? 0 : mapping.getWeeklyPeriods();
    }

    static String key(Long sectionId, Long mappingId) {
        return sectionId + ":" + mappingId;
    }

    private static LocalDate effectiveFrom(TeacherAllocationTeacher row) {
        return row.getEffectiveFrom() == null ? LocalDate.MIN : row.getEffectiveFrom();
    }

    private static Long idOf(Staff staff) {
        return staff == null ? null : staff.getStaffId();
    }

    private static TeacherRef teacherRef(Staff staff) {
        return TeacherRef.builder().staffId(staff.getStaffId()).name(name(staff)).staffCode(staff.getStaffCode()).build();
    }

    static String name(Staff staff) {
        String middle = staff.getMiddleName() == null || staff.getMiddleName().isBlank() ? "" : " " + staff.getMiddleName();
        return (staff.getFirstName() + middle + " " + staff.getLastName()).trim();
    }

    private TeacherCoveragePreviewResponse toPreview(Plan plan) {
        return TeacherCoveragePreviewResponse.builder()
                .valid(true)
                .requiresConfirmation(plan.requiresConfirmation())
                .affectedSections(plan.affected())
                .workloadWarnings(plan.warnings())
                .impacts(plan.impacts())
                .build();
    }

    /** One applicable (section, mapping) pair in the selected group. */
    record Slot(AcademicSection section, AcademicGroupSubjectMapping mapping) {
        String key() {
            return TeacherCoverageServiceImpl.key(section.getSectionId(), mapping.getAcademicGroupSubjectMappingId());
        }
    }

    /** Current database state plus a working copy that the pending changes are applied to. */
    static final class Context {
        AcademicGroup group;
        AcademicYear year;
        List<AcademicSection> sections = List.of();
        final Map<Long, AcademicGroupSubjectMapping> mappings = new LinkedHashMap<>();
        final Map<String, Slot> slots = new LinkedHashMap<>();
        final Map<String, TeacherAllocation> currentAllocation = new HashMap<>();
        final Map<String, Staff> currentStaff = new HashMap<>();
        final Map<String, Source> currentSource = new HashMap<>();
        final Map<Long, Staff> currentGroupStaff = new HashMap<>();
        final Map<String, Staff> workingStaff = new HashMap<>();
        final Map<String, Source> workingSource = new HashMap<>();
        final Map<Long, Staff> workingGroupStaff = new HashMap<>();
        final Map<Long, Integer> keptOverrides = new LinkedHashMap<>();
        final Map<Long, Integer> yearLoad = new HashMap<>();
        final Map<Long, Staff> staffById = new HashMap<>();
        int maxPeriods = FALLBACK_MAX_WEEKLY_PERIODS;

        List<Slot> slotsOf(Long mappingId) {
            return slots.values().stream()
                    .filter(s -> s.mapping().getAcademicGroupSubjectMappingId().equals(mappingId))
                    .toList();
        }
    }

    private record Plan(Context ctx, List<AffectedSection> affected, List<WorkloadWarning> warnings,
                        List<Impact> impacts, boolean requiresConfirmation) {
    }
}
