package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.TeacherArrangementRequest;
import com.thinkerscave.academics.dto.response.TeacherArrangementResponse;
import com.thinkerscave.academics.entity.TeacherArrangement;
import com.thinkerscave.academics.entity.TimetableSlot;
import com.thinkerscave.academics.enums.ArrangementStatus;
import com.thinkerscave.academics.repository.TeacherArrangementRepository;
import com.thinkerscave.academics.repository.TimetableSlotRepository;
import com.thinkerscave.academics.service.TeacherArrangementService;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherArrangementServiceImpl implements TeacherArrangementService {

    private final TeacherArrangementRepository arrangementRepository;
    private final TimetableSlotRepository slotRepository;

    @Override
    @Transactional
    public TeacherArrangementResponse create(TeacherArrangementRequest request) {
        if (request.getAbsentTeacherId().equals(request.getSubstituteTeacherId())) {
            throw new BadRequestException("Absent teacher and substitute teacher cannot be the same");
        }
        TimetableSlot slot = slotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Timetable slot not found: " + request.getSlotId()));

        TeacherArrangement arrangement = new TeacherArrangement();
        arrangement.setTimetableSlot(slot);
        arrangement.setAbsentTeacherId(request.getAbsentTeacherId());
        arrangement.setSubstituteTeacherId(request.getSubstituteTeacherId());
        arrangement.setArrangementDate(request.getArrangementDate());
        arrangement.setReason(request.getReason());
        arrangement.setStatus(ArrangementStatus.PENDING);
        arrangement.setActive(true);
        return toResponse(arrangementRepository.save(arrangement));
    }

    @Override
    @Transactional
    public TeacherArrangementResponse approve(Long arrangementId, Long approvedBy) {
        TeacherArrangement arrangement = getArrangement(arrangementId);
        if (arrangement.getStatus() != ArrangementStatus.PENDING) {
            throw new BadRequestException("Only PENDING arrangements can be approved");
        }
        arrangement.setStatus(ArrangementStatus.APPROVED);
        arrangement.setApprovedBy(approvedBy);
        return toResponse(arrangementRepository.save(arrangement));
    }

    @Override
    @Transactional
    public TeacherArrangementResponse reject(Long arrangementId) {
        TeacherArrangement arrangement = getArrangement(arrangementId);
        if (arrangement.getStatus() != ArrangementStatus.PENDING) {
            throw new BadRequestException("Only PENDING arrangements can be rejected");
        }
        arrangement.setStatus(ArrangementStatus.REJECTED);
        return toResponse(arrangementRepository.save(arrangement));
    }

    @Override
    public TeacherArrangementResponse getById(Long arrangementId) {
        return toResponse(getArrangement(arrangementId));
    }

    @Override
    public List<TeacherArrangementResponse> getByDate(LocalDate date) {
        return arrangementRepository.findByArrangementDateAndStatusOrderByArrangementDateDesc(date, ArrangementStatus.PENDING)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<TeacherArrangementResponse> getByTeacher(Long teacherId) {
        return arrangementRepository.findByAbsentTeacherIdOrSubstituteTeacherIdOrderByArrangementDateDesc(teacherId, teacherId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ---- helpers ----

    private TeacherArrangement getArrangement(Long id) {
        return arrangementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher arrangement not found: " + id));
    }

    private TeacherArrangementResponse toResponse(TeacherArrangement a) {
        return TeacherArrangementResponse.builder()
                .arrangementId(a.getArrangementId())
                .slotId(a.getTimetableSlot().getSlotId())
                .absentTeacherId(a.getAbsentTeacherId())
                .substituteTeacherId(a.getSubstituteTeacherId())
                .arrangementDate(a.getArrangementDate())
                .status(a.getStatus().name())
                .reason(a.getReason())
                .approvedBy(a.getApprovedBy())
                .createdOn(a.getCreatedOn())
                .build();
    }
}
