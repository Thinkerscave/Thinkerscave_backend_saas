package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.AcademicScheduleRequest;
import com.thinkerscave.academics.dto.request.ClassScheduleAssignmentRequest;
import com.thinkerscave.academics.dto.request.PeriodTemplateRequest;
import com.thinkerscave.academics.dto.request.TimetableTemplateRequest;
import com.thinkerscave.academics.dto.response.AcademicScheduleResponse;
import com.thinkerscave.academics.dto.response.PeriodTemplateResponse;
import com.thinkerscave.academics.dto.response.TimetableTemplateResponse;
import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicSchedule;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.ClassScheduleAssignment;
import com.thinkerscave.academics.entity.PeriodTemplate;
import com.thinkerscave.academics.entity.TimetableTemplate;
import com.thinkerscave.academics.enums.PeriodType;
import com.thinkerscave.academics.repository.AcademicScheduleRepository;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.ClassScheduleAssignmentRepository;
import com.thinkerscave.academics.repository.PeriodTemplateRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.academics.repository.TimetableTemplateRepository;
import com.thinkerscave.academics.service.AcademicScheduleService;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicScheduleServiceImpl implements AcademicScheduleService {

    private final AcademicScheduleRepository scheduleRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TimetableTemplateRepository templateRepository;
    private final PeriodTemplateRepository periodRepository;
    private final ClassScheduleAssignmentRepository classScheduleAssignmentRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;

    // ---- Schedule ----

    @Override
    @Transactional
    public AcademicScheduleResponse createSchedule(AcademicScheduleRequest request) {
        AcademicYear year = getYear(request.getAcademicYearId());
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }
        AcademicSchedule schedule = new AcademicSchedule();
        schedule.setAcademicYear(year);
        schedule.setScheduleName(request.getScheduleName());
        schedule.setStartDate(request.getStartDate());
        schedule.setEndDate(request.getEndDate());
        schedule.setRemarks(request.getRemarks());
        schedule.setActive(true);
        return toScheduleResponse(scheduleRepository.save(schedule));
    }

    @Override
    @Transactional
    public AcademicScheduleResponse updateSchedule(Long scheduleId, AcademicScheduleRequest request) {
        AcademicSchedule schedule = getSchedule(scheduleId);
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }
        schedule.setScheduleName(request.getScheduleName());
        schedule.setStartDate(request.getStartDate());
        schedule.setEndDate(request.getEndDate());
        schedule.setRemarks(request.getRemarks());
        return toScheduleResponse(scheduleRepository.save(schedule));
    }

    @Override
    public AcademicScheduleResponse getScheduleById(Long scheduleId) {
        return toScheduleResponse(getSchedule(scheduleId));
    }

    @Override
    public List<AcademicScheduleResponse> getSchedulesByYear(Long academicYearId) {
        return scheduleRepository.findByAcademicYear_AcademicYearIdOrderByStartDateAsc(academicYearId)
                .stream().map(this::toScheduleResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deactivateSchedule(Long scheduleId) {
        AcademicSchedule schedule = getSchedule(scheduleId);
        schedule.setActive(false);
        scheduleRepository.save(schedule);
    }

    // ---- Template ----

    @Override
    @Transactional
    public TimetableTemplateResponse createTemplate(Long scheduleId, TimetableTemplateRequest request) {
        AcademicSchedule schedule = getSchedule(scheduleId);
        TimetableTemplate template = new TimetableTemplate();
        template.setAcademicSchedule(schedule);
        template.setTemplateName(request.getTemplateName());
        template.setRemarks(request.getRemarks());
        template.setActive(true);
        return toTemplateResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public TimetableTemplateResponse updateTemplate(Long templateId, TimetableTemplateRequest request) {
        TimetableTemplate template = getTemplate(templateId);
        template.setTemplateName(request.getTemplateName());
        template.setRemarks(request.getRemarks());
        return toTemplateResponse(templateRepository.save(template));
    }

    @Override
    public List<TimetableTemplateResponse> getTemplatesBySchedule(Long scheduleId) {
        return templateRepository.findByAcademicSchedule_ScheduleIdOrderByTemplateNameAsc(scheduleId)
                .stream().map(this::toTemplateResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deactivateTemplate(Long templateId) {
        TimetableTemplate template = getTemplate(templateId);
        template.setActive(false);
        templateRepository.save(template);
    }

    // ---- Period ----

    @Override
    @Transactional
    public PeriodTemplateResponse addPeriod(Long templateId, PeriodTemplateRequest request) {
        TimetableTemplate template = getTemplate(templateId);
        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().equals(request.getStartTime())) {
            throw new BadRequestException("Period end time must be after start time");
        }
        // Overlap check via query (type-agnostic overlap)
        // We call with a dummy filter; the real guard is the time range check
        // A manual JPQL overlap check would be better — for now guard via application logic
        if (periodRepository.findByTimetableTemplate_TemplateIdOrderByPeriodNumberAsc(templateId).stream()
                .anyMatch(p -> Boolean.TRUE.equals(p.getActive())
                        && p.getStartTime().isBefore(request.getEndTime())
                        && p.getEndTime().isAfter(request.getStartTime()))) {
            throw new BadRequestException("Period time overlaps with an existing period in this template");
        }
        PeriodTemplate period = new PeriodTemplate();
        period.setTimetableTemplate(template);
        period.setPeriodNumber(request.getPeriodNumber());
        period.setPeriodName(request.getPeriodName());
        period.setStartTime(request.getStartTime());
        period.setEndTime(request.getEndTime());
        period.setPeriodType(PeriodType.valueOf(request.getPeriodType()));
        period.setDisplayOrder(request.getDisplayOrder());
        period.setActive(true);
        return toPeriodResponse(periodRepository.save(period));
    }

    @Override
    @Transactional
    public PeriodTemplateResponse updatePeriod(Long periodId, PeriodTemplateRequest request) {
        PeriodTemplate period = getPeriod(periodId);
        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().equals(request.getStartTime())) {
            throw new BadRequestException("Period end time must be after start time");
        }
        final Long pid = periodId;
        if (periodRepository.findByTimetableTemplate_TemplateIdOrderByPeriodNumberAsc(period.getTimetableTemplate().getTemplateId()).stream()
                .anyMatch(p -> Boolean.TRUE.equals(p.getActive())
                        && !p.getPeriodTemplateId().equals(pid)
                        && p.getStartTime().isBefore(request.getEndTime())
                        && p.getEndTime().isAfter(request.getStartTime()))) {
            throw new BadRequestException("Period time overlaps with an existing period in this template");
        }
        period.setPeriodNumber(request.getPeriodNumber());
        period.setPeriodName(request.getPeriodName());
        period.setStartTime(request.getStartTime());
        period.setEndTime(request.getEndTime());
        period.setPeriodType(PeriodType.valueOf(request.getPeriodType()));
        period.setDisplayOrder(request.getDisplayOrder());
        return toPeriodResponse(periodRepository.save(period));
    }

    @Override
    public List<PeriodTemplateResponse> getPeriodsByTemplate(Long templateId) {
        return periodRepository.findByTimetableTemplate_TemplateIdOrderByPeriodNumberAsc(templateId)
                .stream().map(this::toPeriodResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePeriod(Long periodId) {
        PeriodTemplate period = getPeriod(periodId);
        period.setActive(false);
        periodRepository.save(period);
    }

    // ---- Class-schedule assignment ----

    @Override
    @Transactional
    public void assignScheduleToClass(ClassScheduleAssignmentRequest request) {
        AcademicClass cls = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.getClassId()));
        AcademicSection section = request.getSectionId() != null
                ? sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + request.getSectionId()))
                : null;
        AcademicSchedule schedule = getSchedule(request.getScheduleId());
        TimetableTemplate template = getTemplate(request.getTemplateId());

        ClassScheduleAssignment csa = new ClassScheduleAssignment();
        csa.setAcademicClass(cls);
        csa.setAcademicSection(section);
        csa.setAcademicSchedule(schedule);
        csa.setTimetableTemplate(template);
        csa.setRemarks(request.getRemarks());
        csa.setActive(true);
        classScheduleAssignmentRepository.save(csa);
    }

    @Override
    @Transactional
    public void removeScheduleFromClass(Long assignmentId) {
        ClassScheduleAssignment csa = classScheduleAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Class schedule assignment not found: " + assignmentId));
        csa.setActive(false);
        classScheduleAssignmentRepository.save(csa);
    }

    // ---- helpers ----

    private AcademicYear getYear(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + id));
    }

    private AcademicSchedule getSchedule(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic schedule not found: " + id));
    }

    private TimetableTemplate getTemplate(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable template not found: " + id));
    }

    private PeriodTemplate getPeriod(Long id) {
        return periodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Period template not found: " + id));
    }

    private AcademicScheduleResponse toScheduleResponse(AcademicSchedule s) {
        return AcademicScheduleResponse.builder()
                .scheduleId(s.getScheduleId())
                .academicYearId(s.getAcademicYear().getAcademicYearId())
                .yearCode(s.getAcademicYear().getYearCode())
                .scheduleName(s.getScheduleName())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .active(s.getActive())
                .remarks(s.getRemarks())
                .build();
    }

    private TimetableTemplateResponse toTemplateResponse(TimetableTemplate t) {
        return TimetableTemplateResponse.builder()
                .templateId(t.getTemplateId())
                .scheduleId(t.getAcademicSchedule().getScheduleId())
                .scheduleName(t.getAcademicSchedule().getScheduleName())
                .templateName(t.getTemplateName())
                .active(t.getActive())
                .build();
    }

    private PeriodTemplateResponse toPeriodResponse(PeriodTemplate p) {
        return PeriodTemplateResponse.builder()
                .periodTemplateId(p.getPeriodTemplateId())
                .templateId(p.getTimetableTemplate().getTemplateId())
                .periodNumber(p.getPeriodNumber())
                .periodName(p.getPeriodName())
                .startTime(p.getStartTime())
                .endTime(p.getEndTime())
                .periodType(p.getPeriodType() != null ? p.getPeriodType().name() : null)
                .displayOrder(p.getDisplayOrder())
                .active(p.getActive())
                .build();
    }
}
