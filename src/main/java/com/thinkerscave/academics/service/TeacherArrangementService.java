package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.TeacherArrangementRequest;
import com.thinkerscave.academics.dto.response.TeacherArrangementResponse;

import java.time.LocalDate;
import java.util.List;

public interface TeacherArrangementService {

    TeacherArrangementResponse create(TeacherArrangementRequest request);

    TeacherArrangementResponse approve(Long arrangementId, Long approvedBy);

    TeacherArrangementResponse reject(Long arrangementId);

    TeacherArrangementResponse getById(Long arrangementId);

    List<TeacherArrangementResponse> getByDate(LocalDate date);

    List<TeacherArrangementResponse> getByTeacher(Long teacherId);
}
