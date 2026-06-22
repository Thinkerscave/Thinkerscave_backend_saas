package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.AcademicScheduleRequest;
import com.thinkerscave.academics.dto.request.ClassScheduleAssignmentRequest;
import com.thinkerscave.academics.dto.request.PeriodTemplateRequest;
import com.thinkerscave.academics.dto.request.TimetableTemplateRequest;
import com.thinkerscave.academics.dto.response.AcademicScheduleResponse;
import com.thinkerscave.academics.dto.response.PeriodTemplateResponse;
import com.thinkerscave.academics.dto.response.TimetableTemplateResponse;

import java.util.List;

public interface AcademicScheduleService {

    // Schedule
    AcademicScheduleResponse createSchedule(AcademicScheduleRequest request);

    AcademicScheduleResponse updateSchedule(Long scheduleId, AcademicScheduleRequest request);

    AcademicScheduleResponse getScheduleById(Long scheduleId);

    List<AcademicScheduleResponse> getSchedulesByYear(Long academicYearId);

    void deactivateSchedule(Long scheduleId);

    // Timetable template
    TimetableTemplateResponse createTemplate(Long scheduleId, TimetableTemplateRequest request);

    TimetableTemplateResponse updateTemplate(Long templateId, TimetableTemplateRequest request);

    List<TimetableTemplateResponse> getTemplatesBySchedule(Long scheduleId);

    void deactivateTemplate(Long templateId);

    // Period template
    PeriodTemplateResponse addPeriod(Long templateId, PeriodTemplateRequest request);

    PeriodTemplateResponse updatePeriod(Long periodId, PeriodTemplateRequest request);

    List<PeriodTemplateResponse> getPeriodsByTemplate(Long templateId);

    void deletePeriod(Long periodId);

    // Class-schedule assignment
    void assignScheduleToClass(ClassScheduleAssignmentRequest request);

    void removeScheduleFromClass(Long assignmentId);
}
