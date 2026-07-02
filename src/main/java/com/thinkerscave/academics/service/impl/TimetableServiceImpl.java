package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.TimetableSlotRequest;
import com.thinkerscave.academics.dto.response.TimetableResponse;
import com.thinkerscave.academics.dto.response.TimetableSlotResponse;
import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.PeriodTemplate;
import com.thinkerscave.academics.entity.SubjectAssignment;
import com.thinkerscave.academics.entity.TimetableSlot;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.PeriodTemplateRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.academics.repository.SubjectAssignmentRepository;
import com.thinkerscave.academics.repository.TimetableSlotRepository;
import com.thinkerscave.academics.service.TimetableService;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimetableServiceImpl implements TimetableService {

    private final TimetableSlotRepository slotRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final PeriodTemplateRepository periodRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;

    @Override
    @Transactional
    public TimetableSlotResponse createSlot(TimetableSlotRequest request) {
        AcademicClass cls = getClass(request.getClassId());
        AcademicSection section = request.getSectionId() != null ? getSection(request.getSectionId()) : null;
        PeriodTemplate period = getPeriod(request.getPeriodTemplateId());
        SubjectAssignment subjectAssignment = getSubjectAssignment(request.getSubjectAssignmentId());
        DayOfWeek dow = DayOfWeek.valueOf(request.getDayOfWeek());

        // Check teacher conflict (same teacher, same day, same period)
        long conflicts = slotRepository.countTeacherConflict(
                subjectAssignment.getTeacherId(),
                dow,
                period.getPeriodTemplateId(),
                subjectAssignment.getAcademicYear().getAcademicYearId());
        if (conflicts > 0) {
            throw new BadRequestException("Teacher already has a class assigned for this day and period");
        }

        TimetableSlot slot = new TimetableSlot();
        slot.setAcademicYear(subjectAssignment.getAcademicYear());
        slot.setAcademicClass(cls);
        slot.setAcademicSection(section);
        slot.setSubjectAssignment(subjectAssignment);
        slot.setPeriodTemplate(period);
        slot.setDayOfWeek(dow);
        slot.setActive(true);
        return toSlotResponse(slotRepository.save(slot));
    }

    @Override
    @Transactional
    public TimetableSlotResponse updateSlot(Long slotId, TimetableSlotRequest request) {
        TimetableSlot slot = getSlot(slotId);
        PeriodTemplate period = getPeriod(request.getPeriodTemplateId());
        SubjectAssignment subjectAssignment = getSubjectAssignment(request.getSubjectAssignmentId());
        DayOfWeek dow = DayOfWeek.valueOf(request.getDayOfWeek());

        long conflicts = slotRepository.countTeacherConflict(
                subjectAssignment.getTeacherId(),
                dow,
                period.getPeriodTemplateId(),
                slot.getAcademicYear().getAcademicYearId());
        if (conflicts > 0) {
            throw new BadRequestException("Teacher already has a class assigned for this day and period");
        }

        slot.setSubjectAssignment(subjectAssignment);
        slot.setPeriodTemplate(period);
        slot.setDayOfWeek(dow);
        return toSlotResponse(slotRepository.save(slot));
    }

    @Override
    @Transactional
    public void deleteSlot(Long slotId) {
        TimetableSlot slot = getSlot(slotId);
        slot.setActive(false);
        slotRepository.save(slot);
    }

    @Override
    @Transactional(readOnly = true)
    public TimetableResponse getTimetableForClass(Long classId, Long sectionId) {
        AcademicClass cls = getClass(classId);
        AcademicSection section = sectionId != null ? getSection(sectionId) : null;
        List<TimetableSlot> slots = slotRepository
                .findByAcademicClass_ClassIdAndAcademicSection_SectionIdAndActiveOrderByDayOfWeekAscPeriodTemplate_PeriodNumberAsc(
                        classId, sectionId, true);

        Map<String, List<TimetableSlotResponse>> schedule = slots.stream()
                .map(this::toSlotResponse)
                .collect(Collectors.groupingBy(
                        TimetableSlotResponse::getDayOfWeek,
                        LinkedHashMap::new,
                        Collectors.toList()));

        return TimetableResponse.builder()
                .classId(classId)
                .className(cls.getClassName())
                .sectionId(sectionId)
                .sectionName(section != null ? section.getSectionName() : null)
                .schedule(schedule)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableSlotResponse> getTeacherTimetable(Long teacherId, Long academicYearId) {
        return subjectAssignmentRepository
                .findByTeacherIdAndAcademicYear_AcademicYearIdAndActiveTrue(teacherId, academicYearId)
                .stream()
                .flatMap(sa -> slotRepository
                        .findByAcademicClass_ClassIdAndAcademicSection_SectionIdAndActiveOrderByDayOfWeekAscPeriodTemplate_PeriodNumberAsc(
                                sa.getAcademicClass().getClassId(),
                                sa.getAcademicSection() != null ? sa.getAcademicSection().getSectionId() : null,
                                true)
                        .stream()
                        .filter(slot -> slot.getSubjectAssignment().getSubjectAssignmentId()
                                .equals(sa.getSubjectAssignmentId())))
                .map(this::toSlotResponse)
                .collect(Collectors.toList());
    }

    // ---- helpers ----

    private TimetableSlot getSlot(Long id) {
        return slotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable slot not found: " + id));
    }

    private AcademicClass getClass(Long id) {
        return classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + id));
    }

    private AcademicSection getSection(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + id));
    }

    private PeriodTemplate getPeriod(Long id) {
        return periodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Period not found: " + id));
    }

    private SubjectAssignment getSubjectAssignment(Long id) {
        return subjectAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject assignment not found: " + id));
    }

    private TimetableSlotResponse toSlotResponse(TimetableSlot slot) {
        PeriodTemplate pt = slot.getPeriodTemplate();
        SubjectAssignment sa = slot.getSubjectAssignment();
        return TimetableSlotResponse.builder()
                .slotId(slot.getSlotId())
                .dayOfWeek(slot.getDayOfWeek().name())
                .periodNumber(pt.getPeriodNumber())
                .periodName(pt.getPeriodName())
                .startTime(pt.getStartTime())
                .endTime(pt.getEndTime())
                .subjectName(sa.getSubject().getSubjectName())
                .teacherId(sa.getTeacherId())
                .active(slot.getActive())
                .build();
    }
}
