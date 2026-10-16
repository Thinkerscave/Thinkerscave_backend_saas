package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.TeacherArrangementRequest;
import com.thinkerscave.academics.dto.response.TeacherArrangementEligibleSlotResponse;
import com.thinkerscave.academics.dto.response.TeacherArrangementResponse;

import java.time.LocalDate;
import java.util.List;

public interface TeacherArrangementService {
    List<TeacherArrangementResponse> list(Long academicYearId);
    List<TeacherArrangementResponse> listByDate(Long academicYearId, LocalDate absenceDate);
    List<TeacherArrangementEligibleSlotResponse.StaffOption> teachers(Long academicYearId);
    List<TeacherArrangementEligibleSlotResponse> eligibleSlots(Long academicYearId, LocalDate absenceDate, Long absentTeacherId);
    TeacherArrangementResponse create(TeacherArrangementRequest request);
    TeacherArrangementResponse approve(Long arrangementId);
    TeacherArrangementResponse reject(Long arrangementId, String note);
}