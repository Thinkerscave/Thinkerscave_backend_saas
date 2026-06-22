package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.TimetableSlotRequest;
import com.thinkerscave.academics.dto.response.TimetableResponse;
import com.thinkerscave.academics.dto.response.TimetableSlotResponse;

import java.util.List;

public interface TimetableService {

    TimetableSlotResponse createSlot(TimetableSlotRequest request);

    TimetableSlotResponse updateSlot(Long slotId, TimetableSlotRequest request);

    void deleteSlot(Long slotId);

    TimetableResponse getTimetableForClass(Long classId, Long sectionId);

    List<TimetableSlotResponse> getTeacherTimetable(Long teacherId, Long academicYearId);
}
